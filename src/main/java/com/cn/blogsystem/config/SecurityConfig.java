package com.cn.blogsystem.config;

import com.cn.blogsystem.interceptor.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//标记此类为配置类
@Configuration
//开启Spring Security的功能
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserDetailsService userDetailsService;


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // 配置 AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }






    // 3. 配置安全过滤链（核心：接口授权、登录/退出规则）
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（测试环境简化，生产环境需开启）
                .csrf(csrf -> csrf.disable())
                .logout(logout -> logout.disable())
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // ✅ 关键：添加 JWT 过滤器，放在用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 配置接口授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开接口：无需登录即可访问
                        .requestMatchers("/register","/login","/doc.html","/webjars/**", "/v3/api-docs/**").permitAll()
                        // 2. 【关键】静态资源公开 (html, css, js, img)，因为浏览器跳转无法自动带 Token
                        // 这样用户能加载到 index.html 文件，但里面的 JS 请求数据时会被拦截验证
                        .requestMatchers("/*.html", "/js/**", "/css/**", "/images/**", "/static/**").permitAll()
                        // 其他所有接口：需要登录才能访问
                        .anyRequest().authenticated()
                );


        return http.build();
    }


}
