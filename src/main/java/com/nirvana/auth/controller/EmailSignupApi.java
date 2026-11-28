package com.nirvana.auth.controller;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.SignupRequest;
import com.nirvana.auth.dto.signup.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

interface EmailSignupApi {
    @Operation(summary = "이메일 회원가입 요청", description = "가입 요청을 접수하고 Member 프로필 생성을 요청합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "가입 요청 접수",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_REQUEST, INVALID_JSON 또는 INVALID_EMAIL_VERIFICATION_TOKEN",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_USED",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "EMAIL_VERIFICATION_UNAVAILABLE",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "INTERNAL_SERVER_ERROR",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class)))
    })
    ApiResponse<SignupResponse> signup(SignupRequest request);
}
