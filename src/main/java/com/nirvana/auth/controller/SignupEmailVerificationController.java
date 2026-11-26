package com.nirvana.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.email.SendSignupEmailRequest;
import com.nirvana.auth.dto.signup.email.VerifySignupEmailRequest;
import com.nirvana.auth.dto.signup.email.VerifySignupEmailResponse;
import com.nirvana.auth.service.SignupEmailVerificationService;
import com.nirvana.auth.service.SignupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Profile("local & gmail-signup")

public class SignupEmailVerificationController implements SignupEmailVerificationApi {

    private final SignupEmailVerificationService verification;
    private final SignupService signup;

    @Override
    public void send(SendSignupEmailRequest request) {
        signup.sendEmailCode(request.email());
    }

    @Override
    public ApiResponse<VerifySignupEmailResponse> verify(VerifySignupEmailRequest request) {
        var token = verification.verifyCode(request.email(), request.code());
        return ApiResponse.ok(new VerifySignupEmailResponse(token));
    }
}
