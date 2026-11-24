package com.nirvana.auth.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "userAccounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(nullable = false, length = 36)
    private String uuid;

    @Getter
    @Column(length = 50)
    private String loginId;

    @Getter
    @Column(nullable = false)
    private String email;

    @Column(name = "password")
    private String passwordHash;

    @Getter
    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignupType signupType;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime requiredTermsAgreedAt;

    @Column(insertable = false, updatable = false, length = 50)
    private String loginIdActive;

    @Builder(builderMethodName = "generalBuilder")
    public static UserAccount general(
            String loginId,
            String email,
            String passwordHash,
            String phone,
            LocalDateTime requiredTermsAgreedAt
    ) {
        UserAccount account = new UserAccount();
        account.uuid = UUID.randomUUID().toString();
        account.loginId = Objects.requireNonNull(loginId, "loginId");
        account.email = Objects.requireNonNull(email, "email");
        account.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        account.phone = Objects.requireNonNull(phone, "phone");
        account.requiredTermsAgreedAt =
                Objects.requireNonNull(requiredTermsAgreedAt, "requiredTermsAgreedAt");
        account.signupType = SignupType.GENERAL;
        account.status = Status.ACTIVE;
        return account;
    }

    public enum SignupType {
        GENERAL, SOCIAL
    }

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED, WITHDRAWN_PENDING, WITHDRAWN
    }
}
