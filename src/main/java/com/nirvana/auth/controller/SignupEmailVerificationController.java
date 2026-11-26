package com.nirvana.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.email.SendSignupEmailRequest;
import com.nirvana.auth.dto.signup.email.VerifySignupEmailRequest;
import com.nirvana.auth.dto.signup.email.VerifySignupEmailResponse;
import com.nirvana.auth.service.SignupEmailVerificationService;
import com.nirvana.auth.service.SignupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Profile("local & gmail-signup")
@RequestMapping("/signup/email-verification")

public class SignupEmailVerificationController implements SignupEmailVerificationApi {

    private final SignupEmailVerificationService verification;
    private final SignupService signup;

    @Override
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void send(@Valid @RequestBody SendSignupEmailRequest request) {
        signup.sendEmailCode(request.email());
    }

    @Override
    @PostMapping(path = "/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<VerifySignupEmailResponse> verify(@Valid @RequestBody VerifySignupEmailRequest request) {
        var token = verification.verifyCode(request.email(), request.code());
        return ApiResponse.ok(new VerifySignupEmailResponse(token));
    }
}
