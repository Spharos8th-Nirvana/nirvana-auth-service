package com.nirvana.auth.common.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public record ApiError(String code, String message, String traceId,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldViolation> details) {

        public ApiError(String code, String message, String traceId) {
            this(code, message, traceId, List.of());
        }
    }

    public record FieldViolation(String field, String code, String message) {
    }
}
