package com.nirvana.auth.controller;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.SignupRequest;
import com.nirvana.auth.dto.signup.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

interface EmailSignupApi {
    @Operation(summary = "이메일 회원가입")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가입 성공",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_REQUEST, INVALID_JSON 또는 INVALID_EMAIL_VERIFICATION_TOKEN",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_USED",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "INTERNAL_SERVER_ERROR",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class)))
    })
    @PostMapping(path = "/signup", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request);
}
