package com.tradingdiary.exception;

/**
 * 认证失败异常，对应 HTTP 401
 */
public class UnauthorizedException extends BaseException {

    private static final int DEFAULT_CODE = 100101;

    public UnauthorizedException(String message) {
        super(DEFAULT_CODE, message);
    }

    public UnauthorizedException(int code, String message) {
        super(code, message);
    }
}
