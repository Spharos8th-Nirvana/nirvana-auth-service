package com.nirvana.auth.signup.exception;

import com.nirvana.auth.common.web.AuthException;
import com.nirvana.auth.common.web.ErrorCode;

public class EmailAlreadyUsedException extends AuthException {

    public EmailAlreadyUsedException() {
        super(ErrorCode.EMAIL_ALREADY_USED, null);
    }
}
