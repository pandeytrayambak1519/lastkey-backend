package com.lastkey.backend.user.entity;

import com.lastkey.backend.common.enums.AccountStatus;
import com.lastkey.backend.role.entity.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(
                        name = "idx_users_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_users_phone",
                        columnList = "phone"
                ),
                @Index(
                        name = "idx_users_account_locked_until",
                        columnList = "account_locked_until"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            nullable = false,
            length = 50
    )
    private String firstName;

    @Column(
            nullable = false,
            length = 50
    )
    private String lastName;

    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String email;

    @Column(
            nullable = false,
            unique = true,
            length = 15
    )
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus accountStatus =
            AccountStatus.ACTIVE;
    
    private LocalDateTime deactivatedAt;

    private String deactivationReason;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    private LocalDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "role_id",
            nullable = false
    )
    private Role role;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (emailVerified == null) {
            emailVerified = false;
        }

        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }

        if (accountLocked == null) {
            accountLocked = false;
        }

        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}