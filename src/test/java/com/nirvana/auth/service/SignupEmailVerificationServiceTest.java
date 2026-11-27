package com.nirvana.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.nirvana.auth.exception.EmailVerificationException;
import com.nirvana.auth.exception.EmailVerificationException.Reason;
import com.nirvana.auth.repository.SignupVerificationRedisRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@EnabledIfEnvironmentVariable(named = "AUTH_TEST_REDIS_PORT", matches = "[0-9]+")
class SignupEmailVerificationServiceTest {
    private final String email = UUID.randomUUID() + "@example.invalid";
    private final JavaMailSender mailer = mock(JavaMailSender.class);
    private LettuceConnectionFactory connection;
    private StringRedisTemplate redis;
    private SignupEmailVerificationService verification;

    @BeforeEach
    void connect() {
        connection = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("AUTH_TEST_REDIS_PORT")));
        connection.afterPropertiesSet();
        redis = new StringRedisTemplate(connection);
        verification = newService();
    }

    @AfterEach
    void disconnect() {
        try {
            redis.delete(List.of(key("code"), key("token"), key("resend"), key("lease")));
        } finally {
            connection.destroy();
        }
    }

    @Test
    void sharesCodeAndTokenAcrossServicesWithSeparateTtls() {
        String code = sendCode();
        assertThat(redis.getExpire(key("code"), TimeUnit.SECONDS)).isBetween(295L, 300L);
        var second = newService();
        UUID token = second.verifyCode(email, code);
        assertThat(redis.getExpire(key("token"), TimeUnit.SECONDS)).isBetween(1795L, 1800L);
        expect(Reason.INVALID_CODE, () -> verification.verifyCode(email, code));
        expect(Reason.INVALID_VERIFICATION_TOKEN, () -> verification.consumeVerification("other@example.invalid", token));

        verification.consumeVerification(email, token);
        expect(Reason.INVALID_VERIFICATION_TOKEN, () -> second.consumeVerification(email, token));
    }

    @Test
    void rejectsExpiredCredentials() {
        String code = sendCode();
        redis.expireAt(key("code"), Instant.EPOCH);
        expect(Reason.INVALID_CODE, () -> verification.verifyCode(email, code));

        redis.expireAt(key("resend"), Instant.EPOCH);
        UUID token = verification.verifyCode(email, sendCode());
        redis.expireAt(key("token"), Instant.EPOCH);
        expect(Reason.INVALID_VERIFICATION_TOKEN, () -> verification.consumeVerification(email, token));
    }

    @Test
    void enforcesRetryLimitsAcrossServices() {
        String code = sendCode();
        var second = newService();
        expect(Reason.RETRY_LATER, () -> second.sendCode(email));
        for (int attempt = 0; attempt < 5; attempt++) {
            expect(Reason.INVALID_CODE, () -> second.verifyCode(email, "000000"));
        }
        expect(Reason.INVALID_CODE, () -> verification.verifyCode(email, code));
    }

    @Test
    void releasesReservationWhenDeliveryFails() {
        doThrow(new MailSendException("delivery failed")).doNothing().when(mailer).send(any(SimpleMailMessage.class));
        expect(Reason.DELIVERY_FAILED, () -> verification.sendCode(email));
        assertThat(redis.hasKey(key("code"))).isFalse();
        assertThat(redis.hasKey(key("lease"))).isFalse();
        verification.sendCode(email);
    }

    @Test
    void blocksConcurrentUseAndAllowsRetryAfterSaveFailure() {
        UUID token = verification.verifyCode(email, sendCode());
        var second = newService();
        assertThatThrownBy(() -> verification.withVerifiedEmail(email, token, () -> {
            expect(Reason.INVALID_VERIFICATION_TOKEN, () -> second.consumeVerification(email, token));
            throw new IllegalStateException("save failed");
        })).isInstanceOf(IllegalStateException.class);

        second.consumeVerification(email, token);
        expect(Reason.INVALID_VERIFICATION_TOKEN, () -> verification.consumeVerification(email, token));
    }

    private SignupEmailVerificationService newService() {
        return new SignupEmailVerificationService(mailer, "sender@example.invalid",
                new SignupVerificationRedisRepository(redis));
    }

    private String sendCode() {
        org.mockito.Mockito.clearInvocations(mailer);
        verification.sendCode(email);
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailer).send(message.capture());
        return message.getValue().getText().lines().findFirst().orElseThrow().substring("인증번호: ".length());
    }

    private String key(String field) {
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(email.getBytes(StandardCharsets.UTF_8));
        return "auth:signup:{" + encoded + "}:" + field;
    }

    private static void expect(Reason reason, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(
                EmailVerificationException.class, error -> assertThat(error.reason()).isEqualTo(reason));
    }
}
