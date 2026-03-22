package com.cn.blogsystem.service.serviceImpl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cn.blogsystem.common.BusinessException;
import com.cn.blogsystem.dto.ArticleInsertDTO;
import com.cn.blogsystem.dto.ArticleQueryDTO;
import com.cn.blogsystem.dto.ArticleUpdateDTO;
import com.cn.blogsystem.entity.Article;
import com.cn.blogsystem.mapper.ArticleMapper;
import com.cn.blogsystem.service.ArticleService;
import com.cn.blogsystem.vo.ArticleDetailVO;
import com.cn.blogsystem.vo.ArticleListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 缓存 Key 常量
    private static final String HOT_ARTICLES_KEY = "blog:hot:articles";
    // 缓存过期时间 (例如 10 分钟，避免数据长期不一致)
    private static final long CACHE_EXPIRE_MINUTES = 2;


    @Override
    public IPage<ArticleListVO> pageList(ArticleQueryDTO queryDTO) {
        //1.从DTO中获取分页参数
        Page<Article> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        //2.创建lambdaqueryarywrapper对象
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        //3.模糊标题查询
        wrapper.like(StringUtils.hasText(queryDTO.getTitle()), Article::getTitle, queryDTO.getTitle());

        //4.作者ID精确查询
        wrapper.eq(queryDTO.getUserId() != null, Article::getUserId, queryDTO.getUserId());

        // 时间范围查询
        wrapper.ge(queryDTO.getStartTime() != null, Article::getCreateTime, queryDTO.getStartTime());
        wrapper.le(queryDTO.getEndTime() != null, Article::getCreateTime, queryDTO.getEndTime());

        // 按创建时间倒序，最新的在前
        wrapper.orderByDesc(Article::getCreateTime);

        // 3. 执行查询
        Page<Article> articlePage = this.page(page, wrapper);

        // 4. 转换为 VO 返回
        IPage<ArticleListVO> voPage = new Page<>();
        //将 articlePage 中的属性（如 total, size, current, pages 等分页元数据）拷贝到 voPage 中
        BeanUtils.copyProperties(articlePage, voPage, "records");

        //articlePage：是执行 this.page(page, wrapper) 后返回的分页结果对象，它包含了两部分信息：
        //分页元数据：总条数 (total)、当前页码 (current)、每页大小 (size)、总页数 (pages) 等。
        //数据列表：即当前页的具体文章数据。
        //articlePage.getRecords()：专门提取出第 2 部分，即 List<Article> 列表。
        //后续操作：拿到这个列表后，代码通过 .stream() 将其转换为 ArticleListVO 列表，以便返回给前端。
        List<ArticleListVO> voList = articlePage.getRecords().stream().map(article -> {
            ArticleListVO vo = new ArticleListVO();
            BeanUtils.copyProperties(article, vo);
            return vo;
        }).collect(Collectors.toList());//将处理后的流重新收集为一个 List<ArticleListVO> 列表。

        voPage.setRecords(voList);
        return voPage;


    }

    //获取文章详情

    @Override
    public ArticleDetailVO getDetailById(Long id) {
        // ✅ 第一步：定义详情缓存 Key
        String detailKey = "article:detail:" + id;

        // ✅ 第二步：先查 Redis 缓存 (场景一的核心)
        String json = redisTemplate.opsForValue().get(detailKey);

        if (StringUtils.hasText(json)) {
            // --- 【新增核心逻辑开始】 ---
            // 即使命中缓存，也要更新阅读量计数！
            String viewKey = "article:view:" + id;
            Long increment = redisTemplate.opsForValue().increment(viewKey);

            // 如果是第一次创建计数 Key，设置过期时间
            if (increment != null && increment == 1) {
                redisTemplate.expire(viewKey, 24, TimeUnit.HOURS);
            }

            // 解析缓存对象
            ArticleDetailVO vo = JSON.parseObject(json, ArticleDetailVO.class);

            // 计算实时阅读量
            long realTimeViewCount = vo.getViewCount() + (increment - 1);
            vo.setViewCount(realTimeViewCount);

            return vo;
            // --- 【新增核心逻辑结束】 ---

        }

        // ✅ 第三步：未命中，查数据库
        System.out.println("⚠️ 未命中详情缓存，查询数据库 (ID: " + id + ")");
        Article article = this.getById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // ✅ 第四步：处理阅读量计数 (原有的逻辑保留)
        String viewKey = "article:view:" + id;
        Long increment = redisTemplate.opsForValue().increment(viewKey);
        if (increment != null && increment == 1) {
            redisTemplate.expire(viewKey, 24, TimeUnit.HOURS);
        }
        // 计算实时阅读量展示给用户
        long currentViewCount = article.getViewCount() + (increment != null ? increment : 0);
        article.setViewCount(currentViewCount);

        // ✅ 第五步：组装 VO 对象
        ArticleDetailVO vo = new ArticleDetailVO();
        BeanUtils.copyProperties(article, vo);

        // ✅ 第六步：【关键】将详情写入 Redis 缓存 (设置 30 分钟过期)
        // 这样下次再有人访问这篇文章，就直接走第二步返回了
        redisTemplate.opsForValue().set(detailKey, JSON.toJSONString(vo), 30, TimeUnit.MINUTES);

        return vo;
    }


    // 添加文章
    @Override
    public boolean add(ArticleInsertDTO articleDTO) {
        // --- 新增逻辑开始：获取当前登录用户 ID ---
        Long currentUserId = getCurrentUserId();

        String lockKey = "lock:submitArticle:" + currentUserId;

        Boolean lock = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", 3, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(lock)) {
            throw new BusinessException("操作过于频繁，请勿重复提交");
        }


        try {
            //从DTO取用户可编辑字段
            Article article = new Article();
            article.setTitle(articleDTO.getTitle());
            article.setContent(articleDTO.getContent());
            article.setSummary(articleDTO.getSummary());


            // 后端自动赋值（核心安全点）
            article.setUserId(currentUserId); // 从Token获取当前登录用户ID
            article.setViewCount(0L); // 新文章浏览量默认0
            article.setCreateTime(LocalDateTime.now());

            boolean save = this.save(article);
            if (save == true) {
                evictHotArticlesCache(); // ✅ 新增文章后清除缓存
            }



            return save;
        } finally {
            // 【关键】删除锁
            stringRedisTemplate.delete(lockKey);
        }


    }


    // 修改文章
    // 修改后的 update 逻辑示例
    @Override
    public boolean update(ArticleUpdateDTO articleDTO) {
        if (articleDTO.getId() == null) {
            throw new BusinessException("文章 ID 不能为空");
        }

        // 1. 获取当前用户
        Long currentUserId = getCurrentUserId(); // 提取复用逻辑
        String lockKey = "lock:updateArticle:" + currentUserId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 3, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("请勿重复提交");
        }

        try {
            // 2. 查询原文章
            Article oldArticle = this.getById(articleDTO.getId());
            if (oldArticle == null) {
                throw new BusinessException("文章不存在");
            }

            // 3. 【关键】权限校验：必须是作者才能修改
            if (!oldArticle.getUserId().equals(currentUserId)) {
                throw new BusinessException("无权修改该文章");
            }

            // 4. 构建更新对象 (只设可变字段，不要设 userId, createTime 等)
            Article article = new Article();
            article.setId(articleDTO.getId()); // 必须设置 ID
            article.setTitle(articleDTO.getTitle());
            article.setContent(articleDTO.getContent());
            article.setSummary(articleDTO.getSummary());
            // 不需要 setUserId，也不应该让前端或此处随意修改它

            boolean update = this.updateById(article);
            if (update == true) {
                evictHotArticlesCache(); // ✅ 新增文章后清除缓存
            }

            return update;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }


    // 删除文章
    @Override
    public boolean deleteById(Long id) {
        //获取当前登录用户ID
        Long currentUserId = getCurrentUserId();
        String lockKey = "lock:deleteArticle:" + currentUserId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 3, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("请勿重复提交");
        }

        try {
            //获取文章
            Article article = articleMapper.selectById(id);
            if (article == null) {
                return false;
            }

            // 【关键】校验权限：如果不是管理员，且文章作者不是当前用户，则禁止删除
            if (!article.getUserId().equals(currentUserId)) {
                throw new BusinessException("无权删除该文章");
            }
            // 删除
            int delete = articleMapper.deleteById(id);
            if (delete > 0) {
                evictHotArticlesCache(); // ✅ 删除文章后清除缓存
            }

            return delete > 0;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public boolean deleteBatch(Long[] idss) {
        if (idss == null || idss.length == 0) {
            return false;
        }

        // 1. 获取当前登录用户 ID
        Long currentUserId = getCurrentUserId();

        String lockKey = "lock:deleteBatchArticle:" + currentUserId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 3, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("请勿重复提交");
        }

        try {
            // 2. 将数组转为 List 方便查询
            List<Long> ids = new ArrayList<>();
            for (Long id : idss) {
                ids.add(id);
            }

            // 3. 【关键】查询这些 ID 对应的文章实体，用于校验权限
            List<Article> articlesToDelete = this.listByIds(ids);

            // 如果查出来的数量少于传入的数量，说明有 ID 不存在，可根据业务需求决定是否报错
            if (articlesToDelete.size() != ids.size()) {
                throw new BusinessException("部分文章不存在");
            }

            // 4. 【核心安全点】校验每一篇文章是否属于当前用户
            for (Article article : articlesToDelete) {
                if (!article.getUserId().equals(currentUserId)) {
                    // 只要有一篇不是当前用户的，就禁止整个批量操作，防止误删或越权
                    throw new BusinessException("无权删除文章 ID: " + article.getId());
                }
            }

            // 5. 校验通过，执行批量删除
            int deleteBatch = articleMapper.deleteBatchIds(ids);
            if (deleteBatch > 0) {
                evictHotArticlesCache(); // ✅ 删除文章后清除缓存
            }
            return deleteBatch > 0;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }


    // 获取热门文章
    @Override
    public List<ArticleListVO> getHotArticles() {
        // 1. 【先查 Redis】
        String json = redisTemplate.opsForValue().get(HOT_ARTICLES_KEY);

        if (StringUtils.hasText(json)) {
            System.out.println("✅ 命中热门文章缓存");
            return JSON.parseArray(json, ArticleListVO.class);
        }

        // 2. 【未命中，查数据库】
        System.out.println("⚠️ 未命中缓存，查询数据库 (按阅读量排序)");

        // 使用 MyBatis-Plus 构建查询：按阅读量降序，取前 10 条
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Article::getViewCount) // 核心：按阅读量倒序
                .orderByDesc(Article::getCreateTime) // 次要：阅读量相同按时间倒序
                .last("LIMIT 5"); // 限制返回 3 条

        // 执行查询
        List<Article> articles = this.list(wrapper);

        // ✅ 2. 新增转换逻辑：Entity -> VO
        List<ArticleListVO> voList = articles.stream().map(article -> {
            ArticleListVO vo = new ArticleListVO();
            // 只拷贝需要的字段 (id, title, summary, viewCount 等)，自动忽略 content
            BeanUtils.copyProperties(article, vo);
            return vo;
        }).collect(Collectors.toList());


        // 3. 【回写 Redis】
        if (articles != null && !articles.isEmpty()) {
            String articlesJson = JSON.toJSONString(voList);
            // 设置过期时间 10 分钟
            redisTemplate.opsForValue().set(HOT_ARTICLES_KEY, articlesJson, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } else {
            // 防穿透：如果数据库也没数据，存一个空列表，过期时间短一点 (5 分钟)
            redisTemplate.opsForValue().set(HOT_ARTICLES_KEY, "[]", 5, TimeUnit.MINUTES);
        }

        return voList;
    }

    // 三十秒内访问次数不能超过 5 次
    @Override
    public boolean checkRateLimit(String api) {
        Long currentUserId = getCurrentUserId();
        String key = "limit:api" + api + ":" + currentUserId;
        Long count = stringRedisTemplate.opsForValue().increment(key, 1);
        if (count == 1) {
            stringRedisTemplate.expire(key, 30, TimeUnit.SECONDS);
        }
        return count <= 5;
    }


    // ⚠️ 关键：当文章新增、修改、删除时，必须清除缓存，保证数据一致性
    public void evictHotArticlesCache() {
        System.out.println("🧹 清除热门文章缓存");
        redisTemplate.delete(HOT_ARTICLES_KEY);
    }


    //获取当前登录用户 ID
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 防御性检查：防止未登录或上下文为空
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("用户未登录或身份无效");
        }

        String userId = (String) authentication.getPrincipal();
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException("用户身份解析失败，非法的用户 ID 格式：" + userId);
        }
    }


}




