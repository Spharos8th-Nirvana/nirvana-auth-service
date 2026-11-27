package com.nirvana.auth.controller;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.nirvana.auth.exception.GlobalExceptionHandler;
import com.nirvana.auth.service.SignupEmailVerificationService;
import com.nirvana.auth.service.SignupService;
import com.nirvana.auth.service.UserAccountService;

class SignupEmailVerificationControllerTest {

    private static final String PATH = "/signup/email-verification/send";
    private static final String EMAIL = "tester@example.invalid";
    private static final String REQUEST = "{\"email\":\"tester@example.invalid\"}";

    private final UserAccountService accounts = mock(UserAccountService.class);
    private final SignupEmailVerificationService verification = mock(SignupEmailVerificationService.class);
    private final SignupService signup = new SignupService(verification, accounts);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new SignupEmailVerificationController(verification, signup), new EmailSignupController(signup))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void sendsCodeWhenEmailIsAvailable() throws Exception {
        when(accounts.isEmailAvailable(EMAIL)).thenReturn(true);

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNoContent());

        verify(verification).sendCode(EMAIL);
    }

    @Test
    void rejectsDuplicateEmailWithoutSendingCode() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_USED"))
                .andExpect(jsonPath("$.error.details").doesNotExist());

        verifyNoInteractions(verification);
    }

    @Test
    void rejectsInvalidEmailBeforeCheckingAvailability() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        verifyNoInteractions(accounts, verification);
    }

    @Test
    void returnsCommonErrorForMalformedJson() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_JSON"));
        verifyNoInteractions(accounts, verification);
    }

    @Test
    void preservesFrameworkAndAccessStatuses() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/missing")).andExpect(status().isNotFound());

        when(accounts.isEmailAvailable(EMAIL))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isForbidden());
    }

    @Test
    void hidesUnexpectedCauseFromClient() throws Exception {
        when(accounts.isEmailAvailable(EMAIL)).thenThrow(new IllegalStateException("Internal failure"));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }

    @Test
    void identifiesUnacceptedTermsAndPasswordMismatchByInputField() throws Exception {
        mvc.perform(post("/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "email": "tester@example.invalid",
                  "password": "TestSignup1234!",
                  "passwordConfirmation": "DifferentSignup1234!",
                  "name": "Tester",
                  "phone": "01012345678",
                  "requiredTermsAgreed": false,
                  "verificationToken": "5d6acd75-b9bd-4dbc-850a-247fc5c0b549"
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details[?(@.field == 'requiredTermsAgreed')].message")
                        .value(contains("필수 약관에 동의해야 가입할 수 있습니다.")))
                .andExpect(jsonPath("$.error.details[?(@.field == 'passwordConfirmation')].code")
                        .value(contains("AssertTrue")));

        verifyNoInteractions(accounts, verification);
    }
}
