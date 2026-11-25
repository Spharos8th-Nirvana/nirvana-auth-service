package com.nirvana.auth.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.nirvana.auth.dto.signup.SignupRequest;
import com.nirvana.auth.dto.signup.SignupResponse;
import com.nirvana.auth.exception.EmailAlreadyUsedException;

import lombok.RequiredArgsConstructor;

@Service
@Profile("local & gmail-signup")
@RequiredArgsConstructor
public class SignupService {

    private final SignupEmailVerificationService emailVerification;
    private final UserAccountService accounts;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void sendEmailCode(String email) {
        if (!accounts.isEmailAvailable(email)) {
            throw new EmailAlreadyUsedException();
        }
        emailVerification.sendCode(email);
    }

    public SignupResponse signup(SignupRequest request) {
        if (!accounts.isEmailAvailable(request.email())) {
            throw new EmailAlreadyUsedException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        try {
            String userUuid = emailVerification.withVerifiedEmail(request.email(), request.verificationToken(),
                    () -> accounts.createGeneralAccount(request.email(), passwordHash,
                            request.phone(), LocalDateTime.now(ZoneOffset.UTC)));
            return new SignupResponse(userUuid);
        } catch (DataIntegrityViolationException exception) {
            if (!accounts.isEmailAvailable(request.email())) {
                throw new EmailAlreadyUsedException();
            }
            throw exception;
        }
    }
}
