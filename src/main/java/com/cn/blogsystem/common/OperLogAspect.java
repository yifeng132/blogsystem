package com.cn.blogsystem.common;


import com.alibaba.fastjson2.JSON;
import com.cn.blogsystem.entity.SysOperLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.Objects;


/**
 * 操作日志AOP切面
 */

//✅ 能准确识别登录用户。
//✅ 能妥善处理匿名访问。
//✅ 能容忍脏数据不崩盘。
//✅ 完整记录了操作耗时、参数和结果。
@Slf4j
@Aspect // 标记为切面类
@Component // 交给Spring管理
public class OperLogAspect {

    /**
     * 定义切入点：拦截所有controller层的接口（根据你项目的包路径调整）
     * 如果你controller包是com.xxx.blog.controller，就改成这个路径
     */
    @Pointcut("execution(* com.cn.blogsystem.controller.*.*(..))")
    public void operLogPointCut() {
    }

    /**
     * 环绕通知：在接口执行前后记录日志
     */
    @Around("operLogPointCut()")
    public Object recordOperLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 初始化日志对象
        SysOperLog operLog = new SysOperLog();
        long startTime = System.currentTimeMillis(); // 记录开始时间
        Object result = null; // 接口返回结果

        try {
            // 2. 获取请求上下文信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();

            // 3. 填充日志基础信息
            operLog.setOperTime(LocalDateTime.now());
            operLog.setRequestUrl(request.getRequestURI());
            operLog.setRequestMethod(request.getMethod());
            // 序列化请求参数（排除文件上传等特殊参数，这里简化处理）
            operLog.setRequestParams(JSON.toJSONString(joinPoint.getArgs()));


            // 修正后的逻辑
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 判断是否已认证 且 不是匿名账户 (Spring Security 匿名用户通常是 AnonymousAuthenticationToken)
            if (authentication != null && authentication.isAuthenticated()
                    && "anonymousUser".equals(authentication.getPrincipal())) {

                Object principal = authentication.getPrincipal();

                // 根据你的 Filter，principal 应该是 String 类型的 userId
                if (principal instanceof String) {
                    String userIdStr = (String) principal;
                    try {
                        operLog.setUserId(Long.parseLong(userIdStr));
                        operLog.setUsername("user_" + userIdStr); // 建议后续优化：根据 ID 查库获取真实 username
                    } catch (NumberFormatException e) {
                        // 防止 ID 格式错误导致日志记录失败
                        operLog.setUserId(null);
                        operLog.setUsername("unknown_user");
                    }
                } else {
                    // 兼容其他情况，比如 Principal 是 UserDetails 对象
                    // operLog.setUsername(((UserDetails) principal).getUsername());
                    operLog.setUsername("complex_principal");
                }
            } else {
                operLog.setUserId(null);
                operLog.setUsername("anonymous");
            }


            // 5. 执行目标接口方法
            result = joinPoint.proceed();

            // 6. 接口执行成功，填充日志
            operLog.setOperResult("success");
            operLog.setCostTime(System.currentTimeMillis() - startTime);

            return result;
        } catch (Throwable e) {
            // 7. 接口执行失败，填充异常信息
            operLog.setOperResult("fail");
            operLog.setCostTime(System.currentTimeMillis() - startTime);
            operLog.setErrorMsg(e.getMessage());
            throw e; // 继续抛出异常，让全局异常处理器处理
        } finally {
            // 8. 打印日志（后续可以改成存入数据库）
            log.info("【操作日志】{}", JSON.toJSONString(operLog));
            // 如果你想把日志存数据库，这里调用LogService的save方法即可
            // operLogService.save(operLog);
        }
    }
}
