package com.tradingdiary.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 统一 API 响应封装，包含状态码、消息、数据和时间戳
 */
@Getter
public class ApiResponse<T> {

    /** 状态码（200 表示成功） */
    private final int code;

    /** 响应消息 */
    private final String message;

    /** 响应数据 */
    private final T data;

    /** 响应时间戳 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final LocalDateTime timestamp;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(200, message, data);
    }

    public static <Void> ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}