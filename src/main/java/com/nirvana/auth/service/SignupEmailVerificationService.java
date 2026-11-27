package com.nirvana.auth.service;

import com.nirvana.auth.exception.EmailVerificationException;
import com.nirvana.auth.exception.EmailVerificationException.Reason;
import com.nirvana.auth.repository.SignupVerificationRedisRepository;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SignupEmailVerificationService {
    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final SignupVerificationRedisRepository verification;
    private final SecureRandom random = new SecureRandom();

    public SignupEmailVerificationService(JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail,
            SignupVerificationRedisRepository verification) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.verification = verification;
    }

    public void sendCode(String email) {
        UUID owner = UUID.randomUUID();
        if (!verification.reserveSend(email, owner)) {
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
            verification.saveCode(email, owner, code);
        } catch (MailException exception) {
            finish(email, owner, false);
            throw new EmailVerificationException(Reason.DELIVERY_FAILED, exception);
        } catch (EmailVerificationException exception) {
            finish(email, owner, false);
            throw exception;
        }
    }

    public UUID verifyCode(String email, String code) {
        UUID token = UUID.randomUUID();
        if (!verification.verifyCode(email, code, token)) {
            throw new EmailVerificationException(Reason.INVALID_CODE);
        }
        return token;
    }

    public void consumeVerification(String email, UUID token) {
        withVerifiedEmail(email, token, () -> null);
    }

    <T> T withVerifiedEmail(String email, UUID token, Supplier<T> action) {
        UUID owner = UUID.randomUUID();
        if (!verification.reserveToken(email, token, owner)) {
            throw new EmailVerificationException(Reason.INVALID_VERIFICATION_TOKEN);
        }
        boolean completed = false;
        try {
            T result = action.get();
            completed = true;
            return result;
        } finally {
            finish(email, owner, completed);
        }
    }

    private void finish(String email, UUID owner, boolean consumed) {
        try {
            verification.finish(email, owner, consumed);
        } catch (EmailVerificationException exception) {
            log.warn("Could not finalize signup verification in Redis. Lease will expire. consumed={}", consumed);
        }
    }
}
