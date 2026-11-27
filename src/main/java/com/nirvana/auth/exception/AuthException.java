package com.nirvana.auth.exception;

public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AuthException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
