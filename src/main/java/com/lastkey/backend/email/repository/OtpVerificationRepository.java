package com.lastkey.backend.email.repository;

import com.lastkey.backend.email.entity.OtpVerification;
import com.lastkey.backend.email.enums.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification>
    findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            OtpPurpose purpose
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification>
    findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email,
            OtpPurpose purpose
    );

    boolean existsByEmailAndPurposeAndVerifiedTrue(
            String email,
            OtpPurpose purpose
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            update OtpVerification otp
               set otp.used = true,
                   otp.updatedAt = :updatedAt
             where otp.email = :email
               and otp.purpose = :purpose
               and otp.used = false
            """)
    int invalidateExistingOtps(
            @Param("email")
            String email,

            @Param("purpose")
            OtpPurpose purpose,

            @Param("updatedAt")
            LocalDateTime updatedAt
    );

    /*
     * Delete all previous OTP records for the same email and purpose
     * before inserting a new OTP.
     *
     * This prevents unique-constraint conflicts during OTP resend
     * and nominee email changes.
     */
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            delete from OtpVerification otp
             where otp.email = :email
               and otp.purpose = :purpose
            """)
    int deleteByEmailAndPurpose(
            @Param("email")
            String email,

            @Param("purpose")
            OtpPurpose purpose
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            delete from OtpVerification otp
             where otp.expiresAt < :expiryCutoff
            """)
    int deleteExpiredOtps(
            @Param("expiryCutoff")
            LocalDateTime expiryCutoff
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            delete from OtpVerification otp
             where otp.used = true
               and otp.updatedAt < :usedCutoff
            """)
    int deleteOldUsedOtps(
            @Param("usedCutoff")
            LocalDateTime usedCutoff
    );
}