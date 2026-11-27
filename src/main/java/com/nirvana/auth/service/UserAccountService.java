package com.nirvana.auth.service;

import com.nirvana.auth.domain.OutboxEvent;
import com.nirvana.auth.domain.UserAccount;
import com.nirvana.auth.dto.account.AccountDetails;
import com.nirvana.auth.dto.event.ProfileCreationRequested;
import com.nirvana.auth.dto.signup.SignupCommand;
import com.nirvana.auth.repository.OutboxEventRepository;
import com.nirvana.auth.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAccountService {
    private final UserAccountRepository accounts;
    private final OutboxEventRepository outbox;
    private final ObjectMapper json;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public String register(SignupCommand command) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        UserAccount account = UserAccount.generalBuilder()
                .email(command.email())
                .passwordHash(passwordEncoder.encode(command.password()))
                .phone(command.phone())
                .requiredTermsAgreedAt(now)
                .build();
        accounts.save(account);

        String countryCode = command.countryCode() == null ? null : command.countryCode().toUpperCase(Locale.ROOT);
        var event = new ProfileCreationRequested(UUID.randomUUID().toString(), ProfileCreationRequested.TYPE,
                account.getUuid(), command.name().strip(), countryCode);
        outbox.save(new OutboxEvent(event.eventId(), "USER", event.userUuid(), event.eventType(),
                json.writeValueAsString(event), now));
        return account.getUuid();
    }

    public Optional<AccountDetails> findAccount(String uuid) {
        return accounts.findByUuid(uuid)
                .map(account -> new AccountDetails(account.getUuid(), account.getEmail(), account.getPhone()));
    }

    @Transactional
    public void activateRegistration(UUID userUuid) {
        accounts.updateStatus(userUuid.toString(), UserAccount.Status.PENDING_REGISTRATION, UserAccount.Status.ACTIVE);
    }

    public boolean isEmailAvailable(String email) {
        return !accounts.existsByEmailActive(email);
    }
}
