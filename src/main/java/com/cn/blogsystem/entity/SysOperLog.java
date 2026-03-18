package com.cn.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;


/**
 * 系统操作日志实体类
 */
@Data
public class SysOperLog {
    /**
     * 操作人ID
     */
    private Long userId;
    /**
     * 操作人用户名
     */
    private String username;
    /**
     * 操作时间
     */
    private LocalDateTime operTime;
    /**
     * 请求URL
     */
    private String requestUrl;
    /**
     * 请求方式（GET/POST/PUT/DELETE）
     */
    private String requestMethod;
    /**
     * 请求参数
     */
    private String requestParams;
    /**
     * 接口执行耗时（毫秒）
     */
    private Long costTime;
    /**
     * 操作结果（success/fail）
     */
    private String operResult;
    /**
     * 异常信息（失败时填充）
     */
    private String errorMsg;
}

