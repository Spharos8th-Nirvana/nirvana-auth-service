package com.nirvana.auth.signup.service;

import com.nirvana.auth.account.service.UserAccountService;
import com.nirvana.auth.signup.email.service.SignupEmailVerificationService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.nirvana.auth.signup.dto.SignupRequest;
import com.nirvana.auth.signup.dto.SignupResponse;
import com.nirvana.auth.signup.exception.EmailAlreadyUsedException;

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
