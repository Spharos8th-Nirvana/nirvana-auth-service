package com.nirvana.auth.dto.signup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import tools.jackson.databind.json.JsonMapper;

class SignupRequestTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final String PASSWORD = "Valid passphrase1!";

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void acceptsValidInputAndDefaultsOmittedMarketingConsentToFalse() {
        var mapper = JsonMapper.builder().build();
        var request = mapper.readValue("""
            {
              "email": "example@example.invalid",
              "password": "Valid passphrase1!",
              "passwordConfirmation": "Valid passphrase1!",
              "name": "Example",
              "phone": "01000000000",
              "requiredTermsAgreed": true, "verificationToken": "00000000-0000-4000-8000-000000000001"
            }
            """, SignupRequest.class);

        assertThat(FACTORY.getValidator().validate(request)).isEmpty();
        assertThat(request.emailMarketingAgree()).isFalse();
        assertThat(request.smsMarketingAgree()).isFalse();
        assertThat(mapper.writeValueAsString(request)).doesNotContain("password", PASSWORD);
        assertThat(request.toString()).doesNotContain(PASSWORD);
    }

    @Test
    void rejectsMissingOrMalformedFields() {
        var request = new SignupRequest("invalid", null, null, " ", "phone", null, "KOR",
                null, false, false, null);

        assertThat(FACTORY.getValidator().validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email", "password", "passwordConfirmation", "name",
                        "phone", "countryCode", "requiredTermsAgreed", "verificationToken");
    }

    @Test
    void rejectsMismatchedPasswordsAndMissingConsent() {
        var request = request(PASSWORD, "another passphrase", false);

        assertThat(FACTORY.getValidator().validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("passwordConfirmed", "requiredTermsAgreed");
    }

    @Test
    void validatesPasswordCharacterCountAndBcryptByteLimitSeparately() {
        String maxBytes = "Aa1!" + "가".repeat(22) + "aa";
        String overMaxBytes = "Aa1!" + "가".repeat(23);
        assertThat(FACTORY.getValidator().validate(passwordRequest("Aa1!" + "a".repeat(11)))).isEmpty();
        assertThat(FACTORY.getValidator().validate(passwordRequest("Aa1!" + "a".repeat(60)))).isEmpty();
        assertThat(FACTORY.getValidator().validate(passwordRequest(maxBytes))).isEmpty();

        assertThat(FACTORY.getValidator().validate(passwordRequest("Aa1!" + "a".repeat(10))))
                .extracting(ConstraintViolation::getMessage).containsExactly("비밀번호는 15~64자여야 합니다.");
        assertThat(FACTORY.getValidator().validate(passwordRequest("Aa1!" + "a".repeat(61))))
                .extracting(ConstraintViolation::getMessage).containsExactly("비밀번호는 15~64자여야 합니다.");
        assertThat(FACTORY.getValidator().validate(passwordRequest(overMaxBytes)))
                .extracting(ConstraintViolation::getMessage).containsExactly("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lowercase-pass1!",
            "UPPERCASE-PASS1!",
            "MissingNumber!aa",
            "MissingSymbol123",
            "UnlistedSymbol1?"
    })
    void rejectsPasswordsMissingARequiredCharacterType(String password) {
        assertThat(FACTORY.getValidator().validate(passwordRequest(password)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("password");
    }

    private static SignupRequest passwordRequest(String password) {
        return request(password, password, true);
    }

    private static SignupRequest request(String password, String passwordConfirmation, boolean agreed) {
        return new SignupRequest("example@example.invalid", password, passwordConfirmation,
                "Example", "01000000000", null, null, agreed, false, false, UUID.randomUUID());
    }
}
