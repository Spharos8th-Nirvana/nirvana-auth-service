package com.nirvana.auth.exception;

public class EmailVerificationException extends AuthException {

    private final Reason reason;

    public EmailVerificationException(Reason reason) {
        this(reason, null);
    }

    public EmailVerificationException(Reason reason, Throwable cause) {
        super(reason.errorCode, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        INVALID_CODE(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE),
        INVALID_VERIFICATION_TOKEN(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN),
        RETRY_LATER(ErrorCode.EMAIL_RESEND_TOO_SOON),
        DELIVERY_FAILED(ErrorCode.EMAIL_DELIVERY_FAILED);

        private final ErrorCode errorCode;

        Reason(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }
    }
}
