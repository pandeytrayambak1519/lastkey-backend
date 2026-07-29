package com.lastkey.backend.emergency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "emergency_otps",
        indexes = {
                @Index(
                        name = "idx_emergency_otp_request",
                        columnList = "emergency_request_id"
                ),
                @Index(
                        name = "idx_emergency_otp_expires_at",
                        columnList = "expires_at"
                ),
                @Index(
                        name = "idx_emergency_otp_verified",
                        columnList = "verified"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "emergency_request_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_emergency_otp_request"
            )
    )
    private EmergencyRequest emergencyRequest;

    @Column(
            name = "otp_hash",
            nullable = false,
            length = 100
    )
    private String otpHash;

    @Builder.Default
    @Column(
            name = "verification_attempts",
            nullable = false
    )
    private Integer verificationAttempts = 0;

    @Builder.Default
    @Column(
            name = "maximum_attempts",
            nullable = false
    )
    private Integer maximumAttempts = 5;

    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean verified = false;

    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean invalidated = false;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            name = "verified_at"
    )
    private LocalDateTime verifiedAt;

    @Column(
            name = "invalidated_at"
    )
    private LocalDateTime invalidatedAt;

    @Column(
            name = "last_attempt_at"
    )
    private LocalDateTime lastAttemptAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        applyDefaultValues();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();

        applyDefaultValues();
    }

    public boolean isExpired() {

        return expiresAt == null
                || LocalDateTime.now()
                .isAfter(expiresAt);
    }

    public boolean hasAttemptsRemaining() {

        int attempts =
                verificationAttempts == null
                        ? 0
                        : verificationAttempts;

        int allowedAttempts =
                maximumAttempts == null
                        ? 5
                        : maximumAttempts;

        return attempts < allowedAttempts;
    }

    public boolean isUsable() {

        return !Boolean.TRUE.equals(verified)
                && !Boolean.TRUE.equals(invalidated)
                && !isExpired()
                && hasAttemptsRemaining();
    }

    public void registerFailedAttempt() {

        if (verificationAttempts == null) {
            verificationAttempts = 0;
        }

        verificationAttempts++;
        lastAttemptAt = LocalDateTime.now();

        if (!hasAttemptsRemaining()) {
            invalidate();
        }
    }

    public void markVerified() {

        verified = true;
        invalidated = false;
        verifiedAt = LocalDateTime.now();
        lastAttemptAt = verifiedAt;
    }

    public void invalidate() {

        invalidated = true;
        invalidatedAt = LocalDateTime.now();
    }

    private void applyDefaultValues() {

        if (verificationAttempts == null) {
            verificationAttempts = 0;
        }

        if (maximumAttempts == null) {
            maximumAttempts = 5;
        }

        if (verified == null) {
            verified = false;
        }

        if (invalidated == null) {
            invalidated = false;
        }
    }
}