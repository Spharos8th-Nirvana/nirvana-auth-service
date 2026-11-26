package com.nirvana.auth.controller;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.SignupRequest;
import com.nirvana.auth.dto.signup.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

interface EmailSignupApi {
    @Operation(summary = "이메일 회원가입", description = "Auth 계정을 생성합니다. 기본 닉네임은 가입 후 자동 설정됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "계정 생성 완료",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_REQUEST, INVALID_JSON 또는 INVALID_EMAIL_VERIFICATION_TOKEN",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_USED",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "INTERNAL_SERVER_ERROR",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class)))
    })
    ApiResponse<SignupResponse> signup(SignupRequest request);
}
