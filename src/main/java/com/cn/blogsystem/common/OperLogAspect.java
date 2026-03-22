package com.cn.blogsystem.common;


import com.alibaba.fastjson2.JSON;
import com.cn.blogsystem.entity.SysOperLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
import java.util.ArrayList;
import java.util.List;
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

    // 【定义切入点】这里写规则，决定哪些方法需要被记录日志
    // 意思是：拦截 com.cn.blogsystem.controller 包下所有类的所有方法
    @Pointcut("execution(* com.cn.blogsystem.controller..*.*(..))")
    public void pointcut() {}

    // 【环绕通知】这是核心逻辑，像是一个“保镖”，包裹着目标方法执行
    // proceedJoinPoint 代表那个被拦截的“目标方法”（比如 login 方法）
    @Around("pointcut()")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1. 创建一个空的日志对象，准备填数据
        SysOperLog logEntity = new SysOperLog();

        // 2. 记录当前时间戳（毫秒级），用来算后面花了多久
        long startTime = System.currentTimeMillis();

        // 3. 定义一个变量，先存着 null，等会儿用来接目标方法的返回结果
        Object result = null;

        try {
            // 4. 获取当前的 HTTP 请求对象（request），这样才能拿到 URL、参数等信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();

            // 5. 【填数据】把请求的时间、URL、请求方式（GET/POST）填进日志对象
            logEntity.setOperTime(LocalDateTime.now());
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());

            // 6. 【填数据】把接口收到的参数转成 JSON 字符串存起来（方便看传了什么）
            // ✅ 修改为以下安全序列化的逻辑：
            Object[] args = joinPoint.getArgs();
            List<Object> safeArgs = new ArrayList<>();

            for (Object arg : args) {
                // 跳过 Servlet 原生对象，防止序列化报错
                if (arg instanceof HttpServletRequest ||
                        arg instanceof HttpServletResponse ||
                        arg instanceof HttpSession) {
                    continue;
                }
                safeArgs.add(arg);
            }

// 只序列化安全的参数对象
            logEntity.setRequestParams(JSON.toJSONString(safeArgs));

            // 7. 【填数据】尝试获取当前登录用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                // 如果你的 Principal 是 String 类型的 userId
                if (principal instanceof String userIdStr) {
                    logEntity.setUserId(Long.parseLong(userIdStr));
                    logEntity.setUsername("user_" + userIdStr);
                } else {
                    // 如果是其他类型（比如 UserDetails），就取用户名
                    logEntity.setUsername(principal.toString());
                }
            } else {
                // 如果没登录，就记为匿名
                logEntity.setUsername("anonymous");
            }

            // 8. 【关键步骤】执行真正的业务代码（比如执行 UserController.login）
            // 程序运行到这里会暂停，跳进去执行你的业务逻辑，执行完再回到这里
            result = joinPoint.proceed();

            // 9. 业务代码成功执行完了，标记结果为 success
            logEntity.setOperResult("success");

        } catch (Throwable e) {
            // 10. 如果业务代码报错了（抛出异常），捕获它
            // 标记结果为 fail，并把错误信息记下来
            logEntity.setOperResult("fail");
            logEntity.setErrorMsg(e.getMessage());

            // 11. 把异常继续抛出去，别让日志系统把错误“吃掉”了（否则前端收不到报错）
            throw e;
        } finally {
            // 12. 【无论成功还是失败，这里都会执行】
            // 计算耗时：当前时间 - 开始时间
            logEntity.setCostTime(System.currentTimeMillis() - startTime);

            // 13. 把整理好的日志对象转成 JSON，打印到控制台
            // 以后你可以在这里改成 operLogService.save(logEntity) 存到数据库
            log.info("操作日志：{}", JSON.toJSONString(logEntity));
        }

        // 14. 把业务方法的返回结果（result）原封不动地返回给前端
        return result;
    }
}
