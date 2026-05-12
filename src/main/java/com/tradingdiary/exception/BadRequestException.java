package com.tradingdiary.exception;

public class BadRequestException extends BaseException {

    private static final int DEFAULT_CODE = 400001;

    public BadRequestException(String message) {
        super(DEFAULT_CODE, message);
    }

    public BadRequestException(int code, String message) {
        super(code, message);
    }
}
