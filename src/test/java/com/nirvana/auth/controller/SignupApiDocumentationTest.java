package com.nirvana.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nirvana.auth.exception.EmailVerificationException;
import com.nirvana.auth.exception.EmailVerificationException.Reason;
import com.nirvana.auth.service.SignupEmailVerificationService;
import com.nirvana.auth.service.SignupService;
import com.nirvana.auth.service.UserAccountService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles({"local", "gmail-signup"})
@SpringBootTest(properties = {
    "auth.outbox.publishing-enabled=false",
    "spring.mail.username=sender@example.invalid",
    "spring.mail.password=unused-test-password",
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
            + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@ExtendWith(RestDocumentationExtension.class)
class SignupApiDocumentationTest {
    private static final String SEND = "/signup/email-verification/send";
    private static final String VERIFY = "/signup/email-verification/verify";
    private static final String SIGNUP = "/signup";
    private static final String EMAIL = "member@example.invalid";
    private static final UUID TOKEN = UUID.fromString("5d6acd75-b9bd-4dbc-850a-247fc5c0b549");
    private static final String ACCOUNT_UUID = "74497355-d7cb-494f-8220-e9eb186c1e8c";
    private static final String SEND_BODY = """
            {"email":"member@example.invalid"}
            """;
    private static final String VERIFY_BODY = """
            {"email":"member@example.invalid","code":"123456"}
            """;
    private static final String SIGNUP_BODY = """
            {
              "email": "member@example.invalid",
              "password": "ExampleSignup1234!",
              "passwordConfirmation": "ExampleSignup1234!",
              "name": "테스트 회원",
              "phone": "01000000000",
              "requiredTermsAgreed": true,
              "verificationToken": "5d6acd75-b9bd-4dbc-850a-247fc5c0b549"
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserAccountService accounts;

    @MockitoBean
    private SignupEmailVerificationService verification;

    @MockitoSpyBean
    private SignupService signup;

    private MockMvc mvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation).snippets()
                        .withTemplateFormat(TemplateFormats.markdown()))
                .build();
    }

    @Test
    void documentsSuccessfulSendWithoutResponseBody() throws Exception {
        when(accounts.isEmailAvailable(EMAIL)).thenReturn(true);

        submit(SEND, SEND_BODY).andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andDo(document("auth/send/accepted"));
    }

    @Test
    void documentsInvalidSendRequest() throws Exception {
        expectError(submit(SEND, "{}"), 400, "INVALID_REQUEST")
                .andExpect(jsonPath("$.error.details[0].field").value("email"))
                .andDo(document("auth/send/invalid-request"));
    }

    @Test
    void documentsMalformedJson() throws Exception {
        expectError(submit(SEND, "{"), 400, "INVALID_JSON")
                .andDo(document("auth/send/invalid-json"));
    }

    @Test
    void documentsSendToUsedEmail() throws Exception {
        expectError(submit(SEND, SEND_BODY), 409, "EMAIL_ALREADY_USED")
                .andDo(document("auth/send/email-already-used"));
    }

    @ParameterizedTest
    @CsvSource({
        "RETRY_LATER, 429, EMAIL_RESEND_TOO_SOON, resend-too-soon",
        "DELIVERY_FAILED, 503, EMAIL_DELIVERY_FAILED, delivery-failed"
    })
    void documentsEmailSendFailures(Reason reason, int statusCode, String code, String scenario) throws Exception {
        when(accounts.isEmailAvailable(EMAIL)).thenReturn(true);
        doThrow(new EmailVerificationException(reason)).when(verification).sendCode(EMAIL);

        expectError(submit(SEND, SEND_BODY), statusCode, code).andDo(document("auth/send/" + scenario));
    }

    @Test
    void documentsSuccessfulVerification() throws Exception {
        when(verification.verifyCode(EMAIL, "123456")).thenReturn(TOKEN);

        submit(VERIFY, VERIFY_BODY).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationToken").value(TOKEN.toString()))
                .andDo(document("auth/verify/verified"));
    }

    @Test
    void documentsInvalidVerificationCode() throws Exception {
        when(verification.verifyCode(EMAIL, "123456")).thenThrow(new EmailVerificationException(Reason.INVALID_CODE));

        expectError(submit(VERIFY, VERIFY_BODY), 400, "INVALID_EMAIL_VERIFICATION_CODE")
                .andDo(document("auth/verify/invalid-code"));
    }

    @Test
    void documentsSuccessfulSignup() throws Exception {
        when(accounts.isEmailAvailable(EMAIL)).thenReturn(true);
        doReturn(ACCOUNT_UUID).when(signup).signup(any());

        submit(SIGNUP, SIGNUP_BODY).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userUuid").value(ACCOUNT_UUID))
                .andDo(document("auth/signup/created"));
    }

    @Test
    void documentsInvalidSignupToken() throws Exception {
        when(accounts.isEmailAvailable(EMAIL)).thenReturn(true);
        doThrow(new EmailVerificationException(Reason.INVALID_VERIFICATION_TOKEN)).when(signup).signup(any());

        expectError(submit(SIGNUP, SIGNUP_BODY), 400, "INVALID_EMAIL_VERIFICATION_TOKEN")
                .andDo(document("auth/signup/invalid-token"));
    }

    @Test
    void documentsMissingTermsAgreement() throws Exception {
        expectError(submit(SIGNUP, SIGNUP_BODY.replace(": true", ": false")), 400, "INVALID_REQUEST")
                .andExpect(jsonPath("$.error.details[0].field").value("requiredTermsAgreed"))
                .andDo(document("auth/signup/invalid-request"));
    }

    @Test
    void documentsSignupWithUsedEmail() throws Exception {
        expectError(submit(SIGNUP, SIGNUP_BODY), 409, "EMAIL_ALREADY_USED")
                .andDo(document("auth/signup/email-already-used"));
    }

    @Test
    void documentsUnexpectedSignupError() throws Exception {
        when(accounts.isEmailAvailable(EMAIL)).thenThrow(new IllegalStateException("Example unexpected failure"));

        expectError(submit(SIGNUP, SIGNUP_BODY), 500, "INTERNAL_SERVER_ERROR")
                .andDo(document("auth/signup/internal-error"));
    }

    @Test
    void exposesInterfaceDocumentation() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + SEND + "'].post.responses['204'].content").doesNotExist())
                .andExpect(jsonPath("$.paths['" + SEND + "'].post.responses['429']").exists())
                .andExpect(jsonPath("$.paths['" + VERIFY + "'].post.responses['200'].content['application/json']").exists())
                .andExpect(jsonPath("$.paths['" + SIGNUP + "'].post.responses['201'].content['application/json']").exists())
                .andExpect(jsonPath("$.paths['" + SIGNUP + "'].post.responses['409']").exists());
    }

    private ResultActions submit(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions expectError(ResultActions response, int statusCode, String code) throws Exception {
        return response.andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }
}
