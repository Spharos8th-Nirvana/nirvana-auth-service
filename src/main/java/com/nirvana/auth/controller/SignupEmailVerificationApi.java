package com.nirvana.auth.controller;

import com.nirvana.auth.dto.ApiResponse;
import com.nirvana.auth.dto.signup.email.SendSignupEmailRequest;
import com.nirvana.auth.dto.signup.email.VerifySignupEmailRequest;
import com.nirvana.auth.dto.signup.email.VerifySignupEmailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/signup/email-verification")
interface SignupEmailVerificationApi {
    @Operation(summary = "가입 이메일 인증번호 발송")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "인증번호 발송 완료",
                content = @Content()),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_REQUEST 또는 INVALID_JSON",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_USED",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "EMAIL_RESEND_TOO_SOON",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "EMAIL_DELIVERY_FAILED",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "INTERNAL_SERVER_ERROR",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class)))
    })
    @PostMapping(path = "/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void send(@Valid @RequestBody SendSignupEmailRequest request);

    @Operation(summary = "가입 이메일 인증번호 검증")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 인증 증빙 발급",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "INVALID_REQUEST, INVALID_JSON 또는 INVALID_EMAIL_VERIFICATION_CODE",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "INTERNAL_SERVER_ERROR",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignupErrorResponse.class)))
    })
    @PostMapping(path = "/verify", produces = "application/json")
    ApiResponse<VerifySignupEmailResponse> verify(@Valid @RequestBody VerifySignupEmailRequest request);
}
