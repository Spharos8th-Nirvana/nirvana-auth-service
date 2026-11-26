package com.nirvana.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.SignupRequest;
import com.nirvana.auth.dto.signup.SignupResponse;
import com.nirvana.auth.service.SignupService;

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
