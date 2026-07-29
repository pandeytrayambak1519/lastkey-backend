package com.lastkey.backend.user.repository;

import com.lastkey.backend.user.entity.EmailVerificationOtp;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailVerificationOtpRepository
        extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findByUser(User user);

    @Transactional
    @Modifying
    void deleteByUser(User user);
}