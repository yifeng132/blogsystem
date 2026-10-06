package com.cn.blogsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * 不写这个类时，Spring 容器里默认的 RedisTemplate 使用 JDK 自带的序列化器
 * （JdkSerializationRedisSerializer），会导致：
 * 1. 写入 Redis 的 key 和 value 前面带一串 JDK 序列化头（\xac\xed\x00\x05...），
 *    在 redis-cli 里看到的是乱码；
 * 2. keys("article:view:*") 这种通配符查询，pattern 本身也会被 JDK 序列化，
 *    通配符位置被长度字节破坏，永远匹配不到真实 key，定时同步任务失效；
 * 3. RedisTemplate（JDK 序列化）与 StringRedisTemplate（String 序列化）
 *    写入的数据互相不可见。
 *
 * 本项目存的 value 全部是 JSON 字符串，因此 key、value 统一用
 * StringRedisSerializer，和 StringRedisTemplate 行为保持一致。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // key、hashKey 用 String 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        // value、hashValue 用 String 序列化（项目里 value 都是 JSON 字符串）
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
