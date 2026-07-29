package com.lastkey.backend.user.repository;

import com.lastkey.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository
        extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(
            String email
    );

    Optional<User> findByPhone(
            String phone
    );

    boolean existsByEmail(
            String email
    );

    boolean existsByPhone(
            String phone
    );

    boolean existsByPhoneAndIdNot(
            String phone,
            UUID id
    );

    /**
     * Unlocks all temporary account locks whose lock duration
     * has already expired.
     *
     * Accounts manually locked with accountLockedUntil = null
     * are not changed by this query.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE User user
               SET user.accountLocked = false,
                   user.failedLoginAttempts = 0,
                   user.accountLockedUntil = null
             WHERE user.accountLocked = true
               AND user.accountLockedUntil IS NOT NULL
               AND user.accountLockedUntil <= :currentTime
            """)
    int unlockExpiredAccounts(
            @Param("currentTime")
            LocalDateTime currentTime
    );
}