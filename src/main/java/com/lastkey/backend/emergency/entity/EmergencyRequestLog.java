package com.lastkey.backend.emergency.entity;

import com.lastkey.backend.emergency.enums.EmergencyLogAction;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "emergency_request_logs",
        indexes = {
                @Index(
                        name = "idx_emergency_log_request",
                        columnList = "emergency_request_id"
                ),
                @Index(
                        name = "idx_emergency_log_action",
                        columnList = "action"
                ),
                @Index(
                        name = "idx_emergency_log_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyRequestLog {

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
                    name = "fk_emergency_log_request"
            )
    )
    private EmergencyRequest emergencyRequest;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private EmergencyLogAction action;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "previous_status",
            length = 40
    )
    private EmergencyStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "new_status",
            length = 40
    )
    private EmergencyStatus newStatus;

    @Column(
            name = "performed_by",
            nullable = false,
            length = 150
    )
    private String performedBy;

    @Column(
            name = "performed_by_type",
            nullable = false,
            length = 30
    )
    private String performedByType;

    @Column(
            length = 1000
    )
    private String message;

    @Column(
            name = "ip_address",
            length = 50
    )
    private String ipAddress;

    @Column(
            name = "user_agent",
            length = 500
    )
    private String userAgent;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();

        if (performedBy != null) {
            performedBy = performedBy.trim();
        }

        if (performedByType != null) {
            performedByType =
                    performedByType.trim().toUpperCase();
        }

        message = normalizeNullable(message);
        ipAddress = normalizeNullable(ipAddress);
        userAgent = normalizeNullable(userAgent);
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