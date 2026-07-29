package com.lastkey.backend.auth.entity;

import com.lastkey.backend.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "email_verification_tokens",
        indexes = {
                @Index(name = "idx_verification_token_user", columnList = "user_id"),
                @Index(name = "idx_verification_token_otp", columnList = "otp")
        }
)
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 6)
    private String otp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean used = false;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public EmailVerificationToken() {
    }

    public EmailVerificationToken(
            UUID id,
            String otp,
            User user,
            LocalDateTime expiryDate,
            Boolean used,
            Integer attempts,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.otp = otp;
        this.user = user;
        this.expiryDate = expiryDate;
        this.used = used;
        this.attempts = attempts;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();

        if (used == null) {
            used = false;
        }

        if (attempts == null) {
            attempts = 0;
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}