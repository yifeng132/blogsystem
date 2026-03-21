package com.cn.blogsystem.common;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 注入 Redis 模板
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final String SECRET = "EDd+otk8cNR6iAV5M5el0oUMGNCrDe+CwQbDDwExg4g="; // 实际项目中请使用复杂且安全的密钥
    private final long EXPIRATION = 1000 * 60 * 60 * 24; // 令牌有效期24小时

    // 生成JWT令牌
    public String generateToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    // 从令牌中提取user Id
    public String extractUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 验证令牌是否有效
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET).build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取过期时间
     * @param token JWT 令牌
     * @return 过期时间的 Date 对象
     */
    public Date getExpirationDateFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }





    /**
     * 将 Token 加入黑名单
     * @param token 需要失效的 token
     * @param expirationTime 剩余有效时间（毫秒），确保黑名单自动过期
     */
    public void addTokenToBlacklist(String token, long expirationTime) {
        if (expirationTime > 0) {
            stringRedisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "logout",
                    expirationTime,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     * @param token 待检查的 token
     * @return true 如果在黑名单中（已失效），false 如果正常
     */
    //调用 Spring Data Redis 的 hasKey 方法，去 Redis 数据库中检查这个键是否存在。
    // * 如果存在：说明该 Token 之前被调用过 addTokenToBlacklist 方法（用户点击了退出），返回 true。
    // * 如果不存在：说明该 Token 是有效的，或者从未被注销过，返回 false。
    //这里使用 Boolean.TRUE.equals 是为了防止空指针异常，确保最终只返回标准的 true 或 false。
    public boolean isTokenInBlacklist(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    // 辅助方法：获取 Token 剩余过期时间（毫秒）
    public long getRemainingExpiration(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            // 如果 Token 已过期或无效，返回 0，表示无需加入黑名单
            return 0;
        }
    }

}
