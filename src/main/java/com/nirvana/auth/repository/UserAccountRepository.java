package com.nirvana.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nirvana.auth.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUuid(String uuid);

    boolean existsByEmailActive(String email);

    @Modifying
    @Query("update UserAccount a set a.status = :next where a.uuid = :uuid and a.status = :current")
    int updateStatus(@Param("uuid") String uuid, @Param("current") UserAccount.Status current,
                     @Param("next") UserAccount.Status next);
}
