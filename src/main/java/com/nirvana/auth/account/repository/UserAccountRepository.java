package com.nirvana.auth.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nirvana.auth.account.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUuid(String uuid);

    boolean existsByEmailActive(String email);
}
