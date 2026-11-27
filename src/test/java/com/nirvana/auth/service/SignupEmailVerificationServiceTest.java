package com.nirvana.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.nirvana.auth.exception.EmailVerificationException;
import com.nirvana.auth.exception.EmailVerificationException.Reason;

class SignupEmailVerificationServiceTest {

    private static final String EMAIL = "tester@example.invalid";
    private static final String SENDER_EMAIL = "sender@example.invalid";
    private static final Instant NOW = Instant.parse("2026-11-24T00:00:00Z");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final Clock clock = mock(Clock.class);
    private final SignupEmailVerificationService verification =
            new SignupEmailVerificationService(mailSender, SENDER_EMAIL, clock);
    private final ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);

    @Test
    void checksCodeAndConsumesProofExactlyOnceForItsEmail() {
        when(clock.instant()).thenReturn(NOW);
        verification.sendCode(EMAIL);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getFrom()).isEqualTo(SENDER_EMAIL);
        assertThat(mail.getValue().getTo()).containsExactly(EMAIL);

        String code = codeIn(mail.getValue());
        var token = verification.verifyCode(EMAIL, code);
        assertThat(token).isNotNull();
        expect(Reason.INVALID_CODE, () -> verification.verifyCode(EMAIL, code));
        expect(Reason.INVALID_VERIFICATION_TOKEN,
                () -> verification.consumeVerification("other@example.invalid", token));
        verification.consumeVerification(EMAIL, token);
        expect(Reason.INVALID_VERIFICATION_TOKEN, () -> verification.consumeVerification(EMAIL, token));
    }

    @Test
    void rejectsExpiredCodeAndProof() {
        when(clock.instant()).thenReturn(NOW);
        verification.sendCode(EMAIL);
        verification.sendCode("other@example.invalid");
        verification.sendCode("expires@example.invalid");
        verify(mailSender, times(3)).send(mail.capture());
        when(clock.instant()).thenReturn(NOW.plusSeconds(299));
        var proof = verification.verifyCode(EMAIL, codeIn(mail.getAllValues().get(0)));
        var expires = verification.verifyCode("expires@example.invalid", codeIn(mail.getAllValues().get(2)));

        when(clock.instant()).thenReturn(NOW.plusSeconds(300));
        expect(Reason.INVALID_CODE,
                () -> verification.verifyCode("other@example.invalid", codeIn(mail.getAllValues().get(1))));
        when(clock.instant()).thenReturn(NOW.plusSeconds(2098));
        verification.consumeVerification(EMAIL, proof);
        when(clock.instant()).thenReturn(NOW.plusSeconds(2099));
        expect(Reason.INVALID_VERIFICATION_TOKEN,
                () -> verification.consumeVerification("expires@example.invalid", expires));
    }

    @Test
    void limitsCodeAttemptsAndResendRequests() {
        when(clock.instant()).thenReturn(NOW);
        verification.sendCode(EMAIL);
        verify(mailSender).send(mail.capture());
        expect(Reason.RETRY_LATER, () -> verification.sendCode(EMAIL));

        for (int attempt = 0; attempt < 5; attempt++) {
            expect(Reason.INVALID_CODE, () -> verification.verifyCode(EMAIL, "000000"));
        }
        expect(Reason.INVALID_CODE, () -> verification.verifyCode(EMAIL, codeIn(mail.getValue())));

        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        verification.sendCode(EMAIL);
        verify(mailSender, times(2)).send(mail.capture());
        assertThat(verification.verifyCode(EMAIL, codeIn(mail.getValue()))).isNotNull();
    }

    @Test
    void failedDeliveryDoesNotCreateAChallengeOrPreventRetry() {
        when(clock.instant()).thenReturn(NOW);
        doThrow(new MailSendException("test delivery failure")).doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        expect(Reason.DELIVERY_FAILED, () -> verification.sendCode(EMAIL));
        verify(mailSender).send(mail.capture());
        expect(Reason.INVALID_CODE, () -> verification.verifyCode(EMAIL, codeIn(mail.getValue())));
        verification.sendCode(EMAIL);
    }

    @Test
    void savingFailureLeavesProofAvailableForRetry() {
        when(clock.instant()).thenReturn(NOW);
        verification.sendCode(EMAIL);
        verify(mailSender).send(mail.capture());
        var token = verification.verifyCode(EMAIL, codeIn(mail.getValue()));

        assertThatThrownBy(() -> verification.withVerifiedEmail(EMAIL, token, () -> {
            throw new IllegalStateException("save failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(verification.withVerifiedEmail(EMAIL, token, () -> "saved")).isEqualTo("saved");
        expect(Reason.INVALID_VERIFICATION_TOKEN, () -> verification.consumeVerification(EMAIL, token));
    }

    private static String codeIn(SimpleMailMessage message) {
        return message.getText().lines().findFirst().orElseThrow().substring("인증번호: ".length());
    }

    private static void expect(Reason reason, Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(
                EmailVerificationException.class, error -> assertThat(error.reason()).isEqualTo(reason));
    }
}
