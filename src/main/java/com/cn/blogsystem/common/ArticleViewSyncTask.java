
package com.cn.blogsystem.common;

import com.cn.blogsystem.entity.Article;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cn.blogsystem.mapper.ArticleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class ArticleViewSyncTask {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 每 5 分钟执行一次同步任务
     * Cron 表达式：秒 分 时 日 月 周
     * "0 /5 * * * ?" 表示每 5 分钟执行一次 "
     "*/

    @Scheduled(cron = "0 */10 * * * ?")
    public void syncViewCountToDb() {
        System.out.println("🚀 [定时任务] 开始同步文章阅读量到数据库...");

        // 1. 获取所有匹配的文章阅读 Key: article:view:*
        Set<String> keys = redisTemplate.keys("article:view:*");

        if (keys == null || keys.isEmpty()) {
            System.out.println("✅ [定时任务] 暂无需要同步的数据");
            return;
        }

        int successCount = 0;

        // 2. 遍历 Key 进行同步
        for (String key : keys) {
            try {
                // 获取当前的计数值
                String valueStr = redisTemplate.opsForValue().get(key);
                if (!StringUtils.hasText(valueStr)) {
                    continue;
                }

                long increment = Long.parseLong(valueStr);

                // 解析文章 ID: key = "article:view:101" -> id = 101
                String idStr = key.replace("article:view:", "");
                Long articleId = Long.parseLong(idStr);

                // 3. 【核心】使用 MyBatis-Plus 更新数据库 (累加模式)
                // 等价 SQL: UPDATE article SET view_count = view_count + #{increment} WHERE id = #{id}
                LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Article::getId, articleId)
                        .setSql("view_count = view_count + " + increment); // 使用 setSql 进行字段累加

                int update = articleMapper.update(null,updateWrapper);

                if (update > 0) {
                    // 4. 同步成功后，删除 Redis 中的 Key
                    redisTemplate.delete(key);
                    successCount++;

                    // ✅ 3. 【关键新增】删除文章详情缓存！
                    // 强制下次请求去查库，获取包含最新阅读量的数据，并重新建立基准
                    String detailKey = "article:detail:" + articleId;
                    redisTemplate.delete(detailKey);
                    System.out.println("✅ 同步文章 ID: " + articleId + " 阅读量，并已清除详情缓存");
                }
            } catch (Exception e) {
                System.err.println("❌ [定时任务] 同步文章 ID 失败：" + key + ", 错误：" + e.getMessage());
                // 生产环境建议记录日志，不要中断整个循环
            }
        }

        // 5. 如果有文章阅读量被更新了，主动清除热门缓存，保证下次查询是最新的
        if (successCount > 0) {
            // 如果有文章阅读量被更新了，主动清除热门缓存，保证下次查询是最新的
            // 这里需要注入 ArticleService 或者直接使用 redisTemplate 删除 key
            redisTemplate.delete("blog:hot:articles");
            System.out.println("🧹 阅读量同步完成，已清除热门文章缓存");
        }

        System.out.println("✅ [定时任务] 同步完成，成功更新 " + successCount + " 篇文章");
    }
}

