package com.cn.blogsystem.interceptor;

import com.cn.blogsystem.common.BusinessException;
import com.cn.blogsystem.common.JwtUtil;
import com.cn.blogsystem.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // 定义无需认证的路径白名单（可根据实际需求扩展）
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/login",          // 登录接口
            "/register",       // 注册接口
            "/captcha",        // 验证码接口
            "/error",           // Spring Boot 默认的错误页面
            "/favicon.ico", // 浏览器会解析页面。为了在浏览器标签页上显示一个小图标（favicon），它会自动发起一个请求去获取 /favicon.ico
            "/doc.html"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {


        // 第一步：判断当前请求是否在白名单中，若是则直接放行，不执行 Token 验证
        String requestURI = request.getRequestURI();
        if (isInWhiteList(requestURI)) {
            filterChain.doFilter(request, response);
            return; // 直接返回，不执行后续的 Token 验证逻辑
        }


        // 1. 获取请求头中的 Token
        String token = resolveToken(request);

        // 2.如果 Token 存在，则进行解析和认证
        if (StringUtils.hasText(token)) {
            try {
                boolean result = jwtUtil.validateToken(token);
                if ( result==true){
                    // 【新增】检查黑名单
                    if (jwtUtil.isTokenInBlacklist(token)) {
                        // 如果在黑名单中，直接抛出异常或返回 401，阻止后续流程
                        throw new BusinessException(401, "Token 已失效，请重新登录");
                    }

                }




                // 从载荷中获取用户 ID
                String userId = jwtUtil.extractUserId(token);
                if (userId != null) {
                    // 构建权限列表 (当前为空，后续可扩展角色)
                    List<SimpleGrantedAuthority> authorities = Collections.emptyList();

                    // 构建 Authentication 对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    // ✅ 【关键修改】将原始 Token 存入 details 字段，供后续业务（如登出）使用
                    authentication.setDetails(token);


                    // 将认证对象放入 Security 上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("✅ 用户 [" + userId + "] 认证成功，已放入上下文");
                } else {
                    System.out.println("⚠️ Token 解析出的 userId 为 null");
                }
            } catch (Exception e) {
                System.out.println("❌ Token 解析异常：" + e.getMessage());
                e.printStackTrace(); // 打印堆栈，方便排查 403/401 问题
                SecurityContextHolder.clearContext();
            }
        } else {
            System.out.println("⚠️ 请求头中未找到 Token");
        }

        // 4. 继续执行过滤链（无论 Token 是否有效，都让请求继续走后续过滤器/接口）
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Header 中提取 Token
     * 格式：Authorization: Bearer <token>
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


    // 核心方法：判断请求路径是否在白名单中（支持通配符 **）
    private boolean isInWhiteList(String requestURI) {
        // 使用 AntPathMatcher 匹配路径（Spring 内置的路径匹配器，支持通配符）
        AntPathMatcher pathMatcher = new AntPathMatcher();
        for (String whitePath : WHITE_LIST) {
            if (pathMatcher.match(whitePath, requestURI)) {
                return true;
            }
        }
        return false;
    }

}

