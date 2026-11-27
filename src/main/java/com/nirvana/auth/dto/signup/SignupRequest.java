package com.nirvana.auth.dto.signup;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
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
        String password,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank String passwordConfirmation,
        @NotBlank @Size(max = 100) String name,
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

    private static final Set<String> COUNTRY_CODES = Set.of(Locale.getISOCountries());
    private static final java.util.regex.Pattern INTERNATIONAL_NAME =
            java.util.regex.Pattern.compile("[\\p{L}\\p{M}'’.-]+(?: +[\\p{L}\\p{M}'’.-]+)*");
    private static final java.util.regex.Pattern HANGUL =
            java.util.regex.Pattern.compile("\\p{IsHangul}");

    public SignupRequest {
        emailMarketingAgree = Boolean.TRUE.equals(emailMarketingAgree);
        smsMarketingAgree = Boolean.TRUE.equals(smsMarketingAgree);
    }

    @JsonIgnore
    @AssertTrue(message = "비밀번호는 8~64자여야 합니다.")
    public boolean isPasswordLengthValid() {
        if (password == null || password.isBlank()) {
            return true;
        }
        int length = password.codePointCount(0, password.length());
        return length >= 8 && length <= 64;
    }

    @JsonIgnore
    @AssertTrue(message = "비밀번호에 영문 대문자, 소문자, 숫자, 특수문자(!@#$%^&*) 중 3종류 이상을 포함해주세요.")
    public boolean isPasswordCompositionValid() {
        if (password == null || password.isBlank()) {
            return true;
        }
        int categories = 0;
        categories += password.chars().anyMatch(c -> c >= 'A' && c <= 'Z') ? 1 : 0;
        categories += password.chars().anyMatch(c -> c >= 'a' && c <= 'z') ? 1 : 0;
        categories += password.chars().anyMatch(c -> c >= '0' && c <= '9') ? 1 : 0;
        categories += password.chars().anyMatch(c -> "!@#$%^&*".indexOf(c) >= 0) ? 1 : 0;
        return categories >= 3;
    }

    @JsonIgnore
    @AssertTrue(message = "이름은 선택한 국가의 이름 형식에 맞아야 합니다.")
    public boolean isNameValid() {
        if (name == null || name.isBlank()) {
            return true;
        }
        if ("KR".equalsIgnoreCase(countryCode)) {
            return name.matches("[가-힣]+");
        }
        return INTERNATIONAL_NAME.matcher(name).matches() && name.codePoints().anyMatch(Character::isLetter)
                && (countryCode == null || !HANGUL.matcher(name).find());
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

    @JsonIgnore
    @AssertTrue(message = "지원하는 ISO 국가 코드를 입력해주세요.")
    public boolean isCountryCodeValid() {
        return countryCode == null || COUNTRY_CODES.contains(countryCode.toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return "SignupRequest";
    }

    public enum Gender {
        MALE, FEMALE
    }
}
