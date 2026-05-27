package com.tradingdiary.exception;

import lombok.Getter;

/**
 * 业务异常基类，携带错误码和消息
 */
@Getter
public abstract class BaseException extends RuntimeException {

    /** 错误码 */
    private final int code;

    protected BaseException(int code, String message) {
        super(message);
        this.code = code;
    }
}
