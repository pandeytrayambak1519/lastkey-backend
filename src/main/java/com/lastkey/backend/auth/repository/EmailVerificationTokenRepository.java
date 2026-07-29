package com.lastkey.backend.auth.repository;

import com.lastkey.backend.auth.entity.EmailVerificationToken;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByUser(User user);

    Optional<EmailVerificationToken> findByUserAndOtp(
            User user,
            String otp
    );

    void deleteByUser(User user);

    boolean existsByUser(User user);
}