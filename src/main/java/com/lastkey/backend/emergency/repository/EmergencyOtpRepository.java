package com.lastkey.backend.emergency.repository;

import com.lastkey.backend.emergency.entity.EmergencyOtp;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyOtpRepository
        extends JpaRepository<EmergencyOtp, UUID> {

    Optional<EmergencyOtp>
    findFirstByEmergencyRequestAndVerifiedFalseAndInvalidatedFalseOrderByCreatedAtDesc(
            EmergencyRequest emergencyRequest
    );

    Optional<EmergencyOtp>
    findFirstByEmergencyRequestOrderByCreatedAtDesc(
            EmergencyRequest emergencyRequest
    );

    boolean existsByEmergencyRequestAndVerifiedTrueAndInvalidatedFalse(
            EmergencyRequest emergencyRequest
    );

    List<EmergencyOtp>
    findByEmergencyRequestAndInvalidatedFalse(
            EmergencyRequest emergencyRequest
    );

    @Modifying
    @Query("""
            update EmergencyOtp otp
               set otp.invalidated = true,
                   otp.invalidatedAt = :invalidatedAt,
                   otp.updatedAt = :invalidatedAt
             where otp.emergencyRequest = :emergencyRequest
               and otp.invalidated = false
               and otp.verified = false
            """)
    int invalidateActiveOtps(
            @Param("emergencyRequest")
            EmergencyRequest emergencyRequest,

            @Param("invalidatedAt")
            LocalDateTime invalidatedAt
    );

    @Modifying
    @Query("""
            update EmergencyOtp otp
               set otp.invalidated = true,
                   otp.invalidatedAt = :invalidatedAt,
                   otp.updatedAt = :invalidatedAt
             where otp.invalidated = false
               and otp.verified = false
               and otp.expiresAt <= :currentTime
            """)
    int invalidateExpiredOtps(
            @Param("currentTime")
            LocalDateTime currentTime,

            @Param("invalidatedAt")
            LocalDateTime invalidatedAt
    );
}