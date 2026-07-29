package com.lastkey.backend.nominee.entity;

import com.lastkey.backend.nominee.enums.NomineeStatus;
import com.lastkey.backend.nominee.enums.RelationshipType;
import com.lastkey.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "nominees",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_nominee_owner_email",
                        columnNames = {
                                "owner_id",
                                "email"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_nominee_owner",
                        columnList = "owner_id"
                ),
                @Index(
                        name = "idx_nominee_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_nominee_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nominee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "first_name",
            nullable = false,
            length = 100
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 100
    )
    private String lastName;

    @Column(
            nullable = false,
            length = 150
    )
    private String email;

    @Column(
            nullable = false,
            length = 20
    )
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private RelationshipType relationship;

    @Column(
            name = "custom_relationship",
            length = 100
    )
    private String customRelationship;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 40
    )
    @Builder.Default
    private NomineeStatus status =
            NomineeStatus.PENDING_VERIFICATION;

    @Column(
            name = "primary_nominee",
            nullable = false
    )
    @Builder.Default
    private Boolean primaryNominee = false;

    @Column(
            name = "email_verified",
            nullable = false
    )
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(
            name = "phone_verified",
            nullable = false
    )
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(
            name = "verification_token",
            unique = true,
            length = 150
    )
    private String verificationToken;

    @Column(
            name = "verification_token_expiry"
    )
    private LocalDateTime verificationTokenExpiry;

    @Column(
            name = "verified_at"
    )
    private LocalDateTime verifiedAt;

    @Column(
            name = "active",
            nullable = false
    )
    @Builder.Default
    private Boolean active = true;

    @Column(
            name = "notes",
            length = 500
    )
    private String notes;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_nominee_owner"
            )
    )
    private User owner;

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

    @PrePersist
    protected void onCreate() {
        normalizeFields();

        LocalDateTime currentTime =
                LocalDateTime.now();

        createdAt = currentTime;
        updatedAt = currentTime;

        if (status == null) {
            status =
                    NomineeStatus.PENDING_VERIFICATION;
        }

        if (primaryNominee == null) {
            primaryNominee = false;
        }

        if (emailVerified == null) {
            emailVerified = false;
        }

        if (phoneVerified == null) {
            phoneVerified = false;
        }

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeFields();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeFields() {
        firstName = normalizeRequired(firstName);
        lastName = normalizeRequired(lastName);

        if (email != null) {
            email = email.trim().toLowerCase();
        }

        if (phone != null) {
            phone = phone.trim();
        }

        customRelationship =
                normalizeNullable(customRelationship);

        notes = normalizeNullable(notes);
    }

    private String normalizeRequired(
            String value
    ) {
        return value == null
                ? null
                : value.trim();
    }

    private String normalizeNullable(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}