package com.tradingdiary.exception;

/**
 * 资源未找到异常，对应 HTTP 404
 */
public class NotFoundException extends BaseException {

    private static final int DEFAULT_CODE = 400002;

    public NotFoundException(String message) {
        super(DEFAULT_CODE, message);
    }

    public NotFoundException(int code, String message) {
        super(code, message);
    }
}
