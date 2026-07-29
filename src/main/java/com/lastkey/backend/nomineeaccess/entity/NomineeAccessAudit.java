package com.lastkey.backend.nomineeaccess.entity;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nomineeaccess.enums.NomineeAccessAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "nominee_access_audits",
        indexes = {
                @Index(
                        name = "idx_nominee_access_audit_nominee",
                        columnList = "nominee_id"
                ),
                @Index(
                        name = "idx_nominee_access_audit_document",
                        columnList = "document_id"
                ),
                @Index(
                        name = "idx_nominee_access_audit_release",
                        columnList = "release_history_id"
                ),
                @Index(
                        name = "idx_nominee_access_audit_created",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomineeAccessAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "nominee_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_nominee_access_audit_nominee"
            )
    )
    private Nominee nominee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            foreignKey = @ForeignKey(
                    name = "fk_nominee_access_audit_document"
            )
    )
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "release_history_id",
            foreignKey = @ForeignKey(
                    name = "fk_nominee_access_audit_release"
            )
    )
    private EmergencyReleaseHistory releaseHistory;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "action",
            nullable = false,
            length = 50
    )
    private NomineeAccessAction action;

    @Column(
            name = "successful",
            nullable = false
    )
    @Builder.Default
    private Boolean successful = true;

    @Column(
            name = "failure_reason",
            length = 500
    )
    private String failureReason;

    @Column(
            name = "ip_address",
            length = 100
    )
    private String ipAddress;

    @Column(
            name = "user_agent",
            length = 1000
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

        if (successful == null) {
            successful = true;
        }

        failureReason =
                normalizeNullable(failureReason);

        ipAddress =
                normalizeNullable(ipAddress);

        userAgent =
                normalizeNullable(userAgent);
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