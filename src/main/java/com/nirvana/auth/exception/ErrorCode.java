package com.nirvana.auth.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_EMAIL_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않거나 만료되었습니다."),
    INVALID_EMAIL_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "이메일 인증을 다시 진행해주세요."),
    EMAIL_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 인증번호를 다시 요청해주세요."),
    EMAIL_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "인증 메일을 보내지 못했습니다. 잠시 후 다시 요청해주세요."),
    EMAIL_VERIFICATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "이메일 인증을 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVALID_SMS_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않거나 만료되었습니다."),
    INVALID_SMS_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "휴대전화 인증을 다시 진행해주세요."),
    SMS_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 인증번호를 다시 요청해주세요."),
    SMS_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "인증 문자를 보내지 못했습니다. 잠시 후 다시 요청해주세요."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값을 확인해주세요."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "JSON 요청 본문을 확인해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return name();
    }

    public String message() {
        return message;
    }
}
