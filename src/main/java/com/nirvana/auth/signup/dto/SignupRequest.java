package com.nirvana.auth.signup.dto;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank
        @Pattern(
                regexp = "(?s)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*]).*",
                message = "비밀번호에 영문 대문자, 소문자, 숫자, 특수문자(!@#$%^&*)를 각각 하나 이상 포함해주세요."
        )
        String password,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank String passwordConfirmation,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String nickname,
        @NotBlank @Size(max = 20) @Pattern(regexp = "\\+?[0-9]+") String phone,
        Gender gender,
        @Pattern(regexp = "[a-zA-Z]{2}") String countryCode,
        @NotNull(message = "필수 약관 동의 여부를 입력해주세요.")
        @AssertTrue(message = "필수 약관에 동의해야 가입할 수 있습니다.")
        Boolean requiredTermsAgreed,
        Boolean emailMarketingAgree,
        Boolean smsMarketingAgree,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotNull UUID verificationToken
) {

    public SignupRequest {
        emailMarketingAgree = Boolean.TRUE.equals(emailMarketingAgree);
        smsMarketingAgree = Boolean.TRUE.equals(smsMarketingAgree);
    }

    @JsonIgnore
    @AssertTrue(message = "비밀번호는 15~64자여야 합니다.")
    public boolean isPasswordLengthValid() {
        if (password == null || password.isBlank()) {
            return true;
        }
        int length = password.codePointCount(0, password.length());
        return length >= 15 && length <= 64;
    }

    @JsonIgnore
    @AssertTrue(message = "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @JsonIgnore
    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        return password == null || passwordConfirmation == null || password.equals(passwordConfirmation);
    }

    @Override
    public String toString() {
        return "SignupRequest";
    }

    public enum Gender {
        MALE, FEMALE
    }
}
