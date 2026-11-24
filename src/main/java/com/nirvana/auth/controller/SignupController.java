package com.nirvana.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.LoginIdAvailabilityRequest;
import com.nirvana.auth.dto.LoginIdAvailabilityResponse;
import com.nirvana.auth.service.UserAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SignupController {

    private final UserAccountService accounts;

    @PostMapping("/signup/login-id-availability")
    public ApiResponse<LoginIdAvailabilityResponse> checkLoginId(
            @Valid @RequestBody LoginIdAvailabilityRequest request) {
        boolean available = accounts.isLoginIdAvailable(request.loginId());
        log.info("Checked signup login ID availability: available={}", available);
        return ApiResponse.ok(new LoginIdAvailabilityResponse(available));
    }
}
