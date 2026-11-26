package com.nirvana.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nirvana.auth.domain.UserAccount;
import com.nirvana.auth.repository.UserAccountRepository;
import com.nirvana.auth.dto.account.AccountDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAccountService {

    private final UserAccountRepository accounts;

    @Transactional
    public String createGeneralAccount(String email, String passwordHash,
                                       String phone, LocalDateTime agreedAt) {
        UserAccount account = UserAccount.generalBuilder()
                .email(email)
                .passwordHash(passwordHash)
                .phone(phone)
                .requiredTermsAgreedAt(agreedAt)
                .build();

        return accounts.save(account).getUuid();
    }

    public Optional<AccountDetails> findAccount(String uuid) {
        return accounts.findByUuid(uuid)
                .map(account -> new AccountDetails(
                        account.getUuid(), account.getEmail(), account.getPhone()
                ));
    }

    public boolean isEmailAvailable(String email) {
        return !accounts.existsByEmailActive(email);
    }
}
