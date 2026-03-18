package com.cn.blogsystem.common;


import lombok.Getter;

/**
 * 自定义业务异常
 * 为了能区分 “系统异常” 和 “业务异常”，我们需要一个自定义异常。比如：用户不存在、余额不足等。
 */
@Getter
public class BusinessException extends RuntimeException{

    private final int code;
    private final String msg;

    public BusinessException(int code, String msg) {
        super(msg); // 父类构造器存储异常信息
        this.code = code;
        this.msg = msg;
    }

    // 快捷构造器，默认500状态码
    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
        this.msg = msg;
    }
}


