package com.cn.blogsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置
 *
 * 桌面端管理页面通过双击 HTML 文件打开（file:// 协议，浏览器给出的 origin 是字符串 "null"），
 * 浏览器会先发送 OPTIONS 预检请求。Spring Security 默认不开启 CORS，预检会被拦截，
 * 导致页面所有 JSON 请求失败。这里统一放开跨域，并在 SecurityConfig 中通过 .cors() 启用。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 使用 OriginPatterns 而不是 Origins，才能匹配 file:// 的 "null" origin
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // 本系统认证走 Authorization 头，不依赖 Cookie，关闭凭证也更简单
        config.setAllowCredentials(false);
        // 预检结果缓存 1 小时，减少 OPTIONS 请求
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
