package com.nirvana.auth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.nirvana.auth.exception.EmailVerificationException;
import com.nirvana.auth.exception.EmailVerificationException.Reason;

@Service
public class SignupEmailVerificationService {

    private static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(30);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS = 5;

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new HashMap<>();

    @Autowired
    public SignupEmailVerificationService(JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail) {
        this(mailSender, senderEmail, Clock.systemUTC());
    }

    SignupEmailVerificationService(JavaMailSender mailSender, String senderEmail, Clock clock) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.clock = clock;
    }

    public synchronized void sendCode(String email) {
        String key = emailKey(email);
        Instant now = clock.instant();
        challenges.values().removeIf(
                challenge -> !challenge.inUse && !challenge.codeExpiresAt.isAfter(now)
                        && !challenge.tokenIsValidAt(now));
        Challenge current = challenges.get(key);
        if (current != null && (current.inUse || now.isBefore(current.resendAt))) {
            throw new EmailVerificationException(Reason.RETRY_LATER);
        }

        String code = Integer.toString(100_000 + random.nextInt(900_000));
        var message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("NIRVANA 회원가입 인증번호");
        message.setText("인증번호: " + code + "\n5분 이내에 입력해주세요.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailVerificationException(Reason.DELIVERY_FAILED, exception);
        }
        Instant sentAt = clock.instant();
        var next = new Challenge(code, sentAt.plus(CODE_VALIDITY), sentAt.plus(RESEND_INTERVAL));
        if (current != null && current.tokenIsValidAt(sentAt)) {
            next.verificationToken = current.verificationToken;
            next.tokenExpiresAt = current.tokenExpiresAt;
        }
        challenges.put(key, next);
    }

    public synchronized UUID verifyCode(String email, String code) {
        Challenge challenge = challenges.get(emailKey(email));
        Instant now = clock.instant();
        if (challenge == null || challenge.inUse || !challenge.codeExpiresAt.isAfter(now)
                || challenge.code == null || challenge.failedAttempts >= MAX_ATTEMPTS) {
            throw new EmailVerificationException(Reason.INVALID_CODE);
        }
        if (!challenge.code.equals(code)) {
            challenge.failedAttempts++;
            throw new EmailVerificationException(Reason.INVALID_CODE);
        }
        challenge.code = null;
        challenge.verificationToken = UUID.randomUUID();
        challenge.tokenExpiresAt = now.plus(TOKEN_VALIDITY);
        return challenge.verificationToken;
    }

    public void consumeVerification(String email, UUID token) {
        withVerifiedEmail(email, token, () -> null);
    }

    <T> T withVerifiedEmail(String email, UUID token, Supplier<T> action) {
        Challenge challenge = reserveVerification(email, token);
        boolean completed = false;
        try {
            T result = action.get();
            completed = true;
            return result;
        } finally {
            finishVerification(challenge, completed);
        }
    }

    private synchronized Challenge reserveVerification(String email, UUID token) {
        Challenge challenge = challenges.get(emailKey(email));
        if (challenge == null || challenge.inUse || !challenge.tokenIsValidAt(clock.instant())
                || !challenge.verificationToken.equals(token)) {
            throw new EmailVerificationException(Reason.INVALID_VERIFICATION_TOKEN);
        }
        challenge.inUse = true;
        return challenge;
    }

    private synchronized void finishVerification(Challenge challenge, boolean completed) {
        if (completed) {
            challenge.verificationToken = null;
            challenge.tokenExpiresAt = null;
            challenge.code = null;
        }
        challenge.inUse = false;
    }

    private static String emailKey(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private static final class Challenge {

        private String code;
        private final Instant codeExpiresAt;
        private final Instant resendAt;
        private int failedAttempts;
        private UUID verificationToken;
        private Instant tokenExpiresAt;
        private boolean inUse;

        private Challenge(String code, Instant codeExpiresAt, Instant resendAt) {
            this.code = code;
            this.codeExpiresAt = codeExpiresAt;
            this.resendAt = resendAt;
        }

        private boolean tokenIsValidAt(Instant now) {
            return verificationToken != null && tokenExpiresAt.isAfter(now);
        }
    }
}
