package com.lastkey.backend.emergency.entity;

import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.enums.EmergencyTriggerType;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "emergency_requests",
        indexes = {
                @Index(
                        name = "idx_emergency_owner",
                        columnList = "owner_id"
                ),
                @Index(
                        name = "idx_emergency_nominee",
                        columnList = "nominee_id"
                ),
                @Index(
                        name = "idx_emergency_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_emergency_release_at",
                        columnList = "scheduled_release_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_emergency_request_owner"
            )
    )
    private User owner;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "nominee_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_emergency_request_nominee"
            )
    )
    private Nominee nominee;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 40
    )
    @Builder.Default
    private EmergencyStatus status =
            EmergencyStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "trigger_type",
            nullable = false,
            length = 40
    )
    private EmergencyTriggerType triggerType;

    @Column(
            name = "reason",
            nullable = false,
            length = 1000
    )
    private String reason;

    @Column(
            name = "evidence_url",
            length = 500
    )
    private String evidenceUrl;

    @Column(
            name = "waiting_period_days",
            nullable = false
    )
    @Builder.Default
    private Integer waitingPeriodDays = 7;

    @Column(
            name = "owner_notified_at"
    )
    private LocalDateTime ownerNotifiedAt;

    @Column(
            name = "scheduled_release_at"
    )
    private LocalDateTime scheduledReleaseAt;

    @Column(
            name = "approved_at"
    )
    private LocalDateTime approvedAt;

    @Column(
            name = "rejected_at"
    )
    private LocalDateTime rejectedAt;

    @Column(
            name = "cancelled_at"
    )
    private LocalDateTime cancelledAt;

    @Column(
            name = "released_at"
    )
    private LocalDateTime releasedAt;

    @Column(
            name = "expired_at"
    )
    private LocalDateTime expiredAt;

    @Column(
            name = "owner_response_message",
            length = 1000
    )
    private String ownerResponseMessage;

    @Column(
            name = "admin_review_message",
            length = 1000
    )
    private String adminReviewMessage;

    @Column(
            name = "active",
            nullable = false
    )
    @Builder.Default
    private Boolean active = true;

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

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = EmergencyStatus.PENDING;
        }

        if (waitingPeriodDays == null) {
            waitingPeriodDays = 7;
        }

        if (active == null) {
            active = true;
        }

        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeFields();
    }

    private void normalizeFields() {

        if (reason != null) {
            reason = reason.trim();
        }

        evidenceUrl = normalizeNullable(evidenceUrl);

        ownerResponseMessage =
                normalizeNullable(ownerResponseMessage);

        adminReviewMessage =
                normalizeNullable(adminReviewMessage);
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