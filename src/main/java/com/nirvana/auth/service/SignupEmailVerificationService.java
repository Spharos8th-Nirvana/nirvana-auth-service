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
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.nirvana.auth.exception.EmailVerificationException;
import com.nirvana.auth.exception.EmailVerificationException.Reason;

@Service
@Profile("local & gmail-signup")
public class SignupEmailVerificationService {

    private static final Duration VALIDITY = Duration.ofMinutes(5);
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
        challenges.values().removeIf(challenge -> !challenge.expiresAt.isAfter(now));
        Challenge current = challenges.get(key);
        if (current != null && now.isBefore(current.resendAt)) {
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
        challenges.put(key, new Challenge(code, sentAt.plus(VALIDITY), sentAt.plus(RESEND_INTERVAL)));
    }

    public synchronized UUID verifyCode(String email, String code) {
        Challenge challenge = challenges.get(emailKey(email));
        if (challenge == null || !challenge.expiresAt.isAfter(clock.instant())
                || challenge.code == null || challenge.failedAttempts >= MAX_ATTEMPTS) {
            throw new EmailVerificationException(Reason.INVALID_CODE);
        }
        if (!challenge.code.equals(code)) {
            challenge.failedAttempts++;
            throw new EmailVerificationException(Reason.INVALID_CODE);
        }
        challenge.code = null;
        challenge.verificationToken = UUID.randomUUID();
        return challenge.verificationToken;
    }

    public void consumeVerification(String email, UUID token) {
        withVerifiedEmail(email, token, () -> null);
    }

    synchronized <T> T withVerifiedEmail(String email, UUID token, Supplier<T> action) {
        Challenge challenge = challenges.get(emailKey(email));
        if (challenge == null || challenge.verificationToken == null
                || !challenge.verificationToken.equals(token) || !challenge.expiresAt.isAfter(clock.instant())) {
            throw new EmailVerificationException(Reason.INVALID_VERIFICATION_TOKEN);
        }
        T result = action.get();
        challenge.verificationToken = null;
        return result;
    }

    private static String emailKey(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private static final class Challenge {

        private String code;
        private final Instant expiresAt;
        private final Instant resendAt;
        private int failedAttempts;
        private UUID verificationToken;

        private Challenge(String code, Instant expiresAt, Instant resendAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.resendAt = resendAt;
        }
    }
}
