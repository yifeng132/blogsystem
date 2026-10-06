package com.cn.blogsystem.config;

import com.cn.blogsystem.interceptor.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
                // 开启 CORS，自动使用 CorsConfig 中的 CorsConfigurationSource（桌面测试页面跨域用）
                .cors(Customizer.withDefaults())
                .logout(logout -> logout.disable())
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // ✅ 关键：添加 JWT 过滤器，放在用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        // 未认证：无token、没有登录凭证，返回401
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=utf-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请提供Token\"}");
                        })
                        // 已登录成功，但是权限不足，返回403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=utf-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"code\":403,\"msg\":\"权限不足\"}");
                        })
                )
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
