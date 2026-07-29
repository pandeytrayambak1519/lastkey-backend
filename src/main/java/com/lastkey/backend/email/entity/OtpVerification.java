package com.lastkey.backend.email.entity;

import com.lastkey.backend.email.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "otp_verifications",
        indexes = {
                @Index(
                        name = "idx_otp_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_otp_email_purpose",
                        columnList = "email, purpose"
                ),
                @Index(
                        name = "idx_otp_email_purpose_used",
                        columnList = "email, purpose, used"
                ),
                @Index(
                        name = "idx_otp_expiry",
                        columnList = "expires_at"
                ),
                @Index(
                        name = "idx_otp_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            nullable = false,
            length = 150
    )
    private String email;

    /*
     * Plain OTP is never stored in the database.
     */
    @Column(
            name = "otp_hash",
            nullable = false,
            length = 255
    )
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private OtpPurpose purpose;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean verified = false;

    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean used = false;

    @Builder.Default
    @Column(
            nullable = false
    )
    private Integer attempts = 0;

    @Builder.Default
    @Column(
            name = "max_attempts",
            nullable = false
    )
    private Integer maxAttempts = 5;

    @Column(
            name = "verified_at"
    )
    private LocalDateTime verifiedAt;

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

    // =========================================================
    // JPA lifecycle methods
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        applyDefaultValues();
        normalizeEmail();
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt =
                LocalDateTime.now();

        applyDefaultValues();
        normalizeEmail();
    }

    // =========================================================
    // Domain helper methods
    // =========================================================

    public boolean isExpired() {

        return expiresAt == null
                || !LocalDateTime.now()
                .isBefore(expiresAt);
    }

    public boolean hasReachedMaximumAttempts() {

        int currentAttempts =
                attempts == null
                        ? 0
                        : attempts;

        int allowedAttempts =
                maxAttempts == null
                        ? 5
                        : maxAttempts;

        return currentAttempts
                >= allowedAttempts;
    }

    public boolean isAvailableForVerification() {

        return !Boolean.TRUE.equals(used)
                && !Boolean.TRUE.equals(verified)
                && !isExpired()
                && !hasReachedMaximumAttempts();
    }

    public void increaseAttempts() {

        if (Boolean.TRUE.equals(used)) {
            return;
        }

        if (this.attempts == null) {
            this.attempts = 0;
        }

        this.attempts++;
    }

    public void markVerified() {

        if (Boolean.TRUE.equals(used)) {

            throw new IllegalStateException(
                    "OTP has already been consumed"
            );
        }

        if (isExpired()) {

            throw new IllegalStateException(
                    "Expired OTP cannot be verified"
            );
        }

        this.verified = true;
        this.used = true;
        this.verifiedAt =
                LocalDateTime.now();
    }

    public void invalidate() {

        this.used = true;
    }

    // =========================================================
    // Internal normalization
    // =========================================================

    private void applyDefaultValues() {

        if (this.verified == null) {
            this.verified = false;
        }

        if (this.used == null) {
            this.used = false;
        }

        if (this.attempts == null
                || this.attempts < 0) {

            this.attempts = 0;
        }

        if (this.maxAttempts == null
                || this.maxAttempts < 1) {

            this.maxAttempts = 5;
        }
    }

    private void normalizeEmail() {

        if (this.email != null) {

            this.email =
                    this.email
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );
        }
    }
}