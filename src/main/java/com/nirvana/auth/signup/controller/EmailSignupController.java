package com.nirvana.auth.signup.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import com.nirvana.auth.common.web.ApiResponse;
import com.nirvana.auth.signup.dto.SignupRequest;
import com.nirvana.auth.signup.dto.SignupResponse;
import com.nirvana.auth.signup.service.SignupService;

import lombok.RequiredArgsConstructor;

@RestController
@Profile("local & gmail-signup")
@RequiredArgsConstructor
public class EmailSignupController implements EmailSignupApi {

    private final SignupService signupService;

    @Override
    public ApiResponse<SignupResponse> signup(SignupRequest request) {
        return ApiResponse.ok(signupService.signup(request));
    }
}
