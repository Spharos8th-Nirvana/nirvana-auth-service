package com.nirvana.auth.exception;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.SignupRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AuthException.class)
    ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException exception) {
        return respond(exception.errorCode(), traceId());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        boolean signup = exception.getBindingResult().getTarget() instanceof SignupRequest;
        var details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiResponse.FieldViolation(
                        signup ? signupInputField(error.getField()) : error.getField(),
                        error.getCode(), error.getDefaultMessage()))
                .toList();
        var errorCode = ErrorCode.INVALID_REQUEST;
        var body = new ApiResponse<Void>(false, null,
                new ApiResponse.ApiError(errorCode.code(), errorCode.message(), traceId(), details));
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, body(ErrorCode.INVALID_JSON, traceId()), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body == null) {
            String traceId = traceId();
            if (status.is5xxServerError()) {
                log.error("Auth request failed. traceId={}", traceId, exception);
                body = body(ErrorCode.INTERNAL_SERVER_ERROR, traceId);
            } else {
                body = frameworkError(status, traceId);
            }
        }
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        String traceId = traceId();
        if (exception instanceof ErrorResponse error && !error.getStatusCode().is5xxServerError()) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                    .body(frameworkError(error.getStatusCode(), traceId));
        }

        log.error("Auth request failed. traceId={}", traceId, exception);
        return respond(ErrorCode.INTERNAL_SERVER_ERROR, traceId);
    }

    private static ResponseEntity<ApiResponse<Void>> respond(ErrorCode errorCode, String traceId) {
        return ResponseEntity.status(errorCode.status()).body(body(errorCode, traceId));
    }

    private static ApiResponse<Void> body(ErrorCode errorCode, String traceId) {
        return new ApiResponse<>(false, null, new ApiResponse.ApiError(errorCode.code(), errorCode.message(), traceId));
    }

    private static ApiResponse<Void> frameworkError(HttpStatusCode status, String traceId) {
        return new ApiResponse<>(false, null,
                new ApiResponse.ApiError("HTTP_" + status.value(), "요청을 처리할 수 없습니다.", traceId));
    }

    private static String signupInputField(String field) {
        return switch (field) {
            case "passwordLengthValid", "passwordWithinBcryptLimit" -> "password";
            case "passwordConfirmed" -> "passwordConfirmation";
            case "countryCodeValid" -> "countryCode";
            default -> field;
        };
    }

    private static String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null ? UUID.randomUUID().toString() : traceId;
    }
}
