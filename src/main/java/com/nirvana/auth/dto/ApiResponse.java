package com.nirvana.auth.dto;

public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public record ApiError(String code, String message, String traceId) {
    }
}
