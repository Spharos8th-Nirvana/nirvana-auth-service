package com.nirvana.auth.service;

import com.nirvana.auth.dto.signup.SignupCommand;
import com.nirvana.auth.exception.EmailAlreadyUsedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupService {
    private final SignupEmailVerificationService emailVerification;
    private final UserAccountService accounts;

    public void sendEmailCode(String email) {
        requireAvailableEmail(email);
        emailVerification.sendCode(email);
    }

    public String signup(SignupCommand command) {
        requireAvailableEmail(command.email()); //중복 이메일 검사
        try {
            return emailVerification.withVerifiedEmail(command.email(), command.verificationToken(),
                    () -> accounts.register(command));  //이메일 검증
        } catch (DataIntegrityViolationException exception) {
            requireAvailableEmail(command.email());
            throw exception;
        }
    }

    private void requireAvailableEmail(String email) {
        if (!accounts.isEmailAvailable(email)) {
            throw new EmailAlreadyUsedException();
        }
    }
}
