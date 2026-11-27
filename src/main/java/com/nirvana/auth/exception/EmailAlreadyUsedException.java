package com.nirvana.auth.exception;

public class EmailAlreadyUsedException extends AuthException {

    public EmailAlreadyUsedException() {
        super(ErrorCode.EMAIL_ALREADY_USED, null);
    }
}
