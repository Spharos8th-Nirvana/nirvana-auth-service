package com.nirvana.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.SignupRequest;
import com.nirvana.auth.dto.signup.SignupResponse;
import com.nirvana.auth.service.SignupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@Profile("local & gmail-signup")
@RequiredArgsConstructor
public class EmailSignupController implements EmailSignupApi {

    private final SignupService signupService;

    @Override
    @PostMapping(path = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(signupService.signup(request));
    }
}
