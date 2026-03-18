package com.cn.blogsystem.interceptor;

import com.cn.blogsystem.common.BusinessException;
import com.cn.blogsystem.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 1. 捕获业务异常（自定义异常）
     * 当你主动抛出 new BusinessException("xxx") 时，会被此方法捕获
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：", e); // 记录日志
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 2. 捕获空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常：", e);
        return Result.error(500, "系统繁忙，空指针异常");
    }

    /**
     * 3. 捕获参数校验失败异常
     * 比如 @RequestParam 或 @RequestBody 参数校验失败
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException e) {
        log.error("参数校验异常：", e);
        // 获取第一个错误信息，也可以拼接所有错误信息
        String errorMsg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(400, errorMsg);
    }

    /**
     * 4. 兜底：捕获所有其他异常（防止系统崩溃）
     * 所有没被上面捕获的异常，都会走到这里
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.error(500, "服务器内部错误，请联系管理员");
    }

}
