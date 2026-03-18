package com.cn.blogsystem.service.serviceImpl;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;


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
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        ArticleDetailVO vo = new ArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }


    // 添加文章
    @Override
    public boolean add(ArticleInsertDTO articleDTO) {
        //从DTO取用户可编辑字段
        Article article = new Article();
        article.setTitle(articleDTO.getTitle());
        article.setContent(articleDTO.getContent());
        article.setSummary(articleDTO.getSummary());

        // --- 新增逻辑开始：获取当前登录用户 ID ---
        Long currentUserId = getCurrentUserId();
        // 后端自动赋值（核心安全点）
        article.setUserId(currentUserId); // 从Token获取当前登录用户ID
        article.setViewCount(0); // 新文章浏览量默认0
        article.setCreateTime(LocalDateTime.now());

        boolean save = this.save(article);
        return save;
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

        return this.updateById(article);
    }


    @Override
    public boolean deleteById(Long id) {
        //获取当前登录用户ID
        Long currentUserId = getCurrentUserId();

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
        return delete > 0;
    }

    @Override
    public boolean deleteBatch(Long[] idss) {
        if (idss == null || idss.length == 0) {
            return false;
        }

        // 1. 获取当前登录用户 ID
        Long currentUserId = getCurrentUserId();

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
        return deleteBatch > 0;
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




//第一步：生成 Token (登录时)
//调用 jwtUtil.generateToken(userId)。
//内部执行 Jwts.builder().setSubject(userId)...。
//此时，用户 ID 被写入了 JWT 的 sub 字段。
//第二步：解析 Token (请求进来时，过滤器中)
//JwtAuthenticationFilter 拦截请求，解析 Token。
//执行 claims.getSubject() 取出里面的用户 ID。
//创建一个 Spring Security 的对象：new UsernamePasswordAuthenticationToken(userId, ...)。
//关键点：这里把取出的 ID 放到了 Principal 的位置。
//存入上下文：SecurityContextHolder.getContext().setAuthentication(authToken)。
//第三步：使用用户信息 (你的当前代码)
//在 add 方法中。
//执行 Authentication auth = SecurityContextHolder.getContext().getAuthentication();。
//执行 auth.getPrincipal()。
//结果：拿到的就是第一步里塞进去的用户 ID。


/**
 * 这行代码是 MyBatis-Plus 中构建动态 SQL 查询条件的经典写法，用于实现"**只有当标题不为空时，才执行模糊查询**"的逻辑。
 * <p>
 * 我们可以将其拆解为三个部分来详细理解：
 * <p>
 * ### 1. 核心方法 `wrapper.like(condition, column, value)`
 * 这是 `LambdaQueryWrapper` 的 `like` 方法重载版本，它接收三个参数：
 * *   **第 1 个参数（条件）**：`StringUtils.hasText(queryDTO.getTitle())`
 * *   **第 2 个参数（列）**：`Article::getTitle`
 * *   **第 3 个参数（值）**：`queryDTO.getTitle()`
 * <p>
 * ### 2. 参数详细解析
 * <p>
 * #### 🔹 第 1 个参数：执行条件
 * ```java
 * StringUtils.hasText(queryDTO.getTitle())
 * ```
 * <p>
 * *   **作用**：这是一个布尔判断（`boolean`）。
 * *   **逻辑**：利用 Spring 的工具类 `StringUtils` 检查 `queryDTO.getTitle()` 是否**有文本内容**（即不为 `null` 且不为空字符串 `""`，也不全是空格）。
 * *   **效果**：
 * *   如果返回 `true`：MyBatis-Plus 会将后面的 `LIKE` 条件拼接到最终的 SQL 语句中。
 * *   如果返回 `false`：MyBatis-Plus 会**忽略**这个条件，不拼接任何 SQL，相当于没写过这行代码。
 * *   **目的**：实现**动态查询**。如果用户没传标题，就不加过滤条件，查出所有数据；如果传了标题，就只查匹配的。
 * <p>
 * #### 🔹 第 2 个参数：数据库字段（Lambda 表达式）
 * ```java
 * Article::getTitle
 * ```
 * <p>
 * *   **作用**：这是一个方法引用（Method Reference），指向实体类的 `getTitle` 方法。
 * *   **优势**：
 * *   **类型安全**：编译器会检查 `getTitle` 是否存在，如果字段名写错（比如改成 `getTtle`），编译直接报错，而不是等到运行时报错。
 * *   **自动映射**：MyBatis-Plus 会自动解析这个方法，找到对应的数据库列名（通常是 [title](file://D:\Java\idea2025.2\IntelliJ%20IDEA%202025.2\code\blogsystem\src\main\java\com\cn\blogsystem\entity\Article.java#L12-L12)，如果配置了驼峰转换或表注解，会自动适配）。避免了手动写字符串 `"title"` 可能出现的拼写错误。
 * <p>
 * #### 🔹 第 3 个参数：查询值
 * ```java
 * queryDTO.getTitle()
 * ```
 * <p>
 * *   **作用**：这是实际要用来匹配的值。
 * *   **SQL 表现**：MyBatis-Plus 会自动在该值的前后加上 `%` 通配符。
 * *   假设传入值为 `"Java"`。
 * *   生成的 SQL 片段会是：`AND title LIKE '%Java%'`。
 * <p>
 * ### 3. 最终生成的 SQL 示例
 * <p>
 * 假设数据库表名为 `article`：
 * <p>
 * *   **场景 A：用户传了标题 "Spring"**
 * *   条件判断为 `true`。
 * *   生成 SQL：
 * ```sql
 * SELECT ... FROM article WHERE title LIKE '%Spring%'
 * ```
 * <p>
 * <p>
 * *   **场景 B：用户没传标题 (null 或 "")**
 * *   条件判断为 `false`。
 * *   生成 SQL（完全忽略 like 条件）：
 * ```sql
 * SELECT ... FROM article
 * -- 注意：这里没有 WHERE title LIKE ...
 * ```
 *
 **/
