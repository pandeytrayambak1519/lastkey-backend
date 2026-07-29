package com.lastkey.backend.emergency.entity;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.nominee.entity.Nominee;
import jakarta.persistence.*;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "emergency_release_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_emergency_release_request_document_nominee",
                        columnNames = {
                                "emergency_request_id",
                                "document_id",
                                "nominee_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_release_history_request",
                        columnList = "emergency_request_id"
                ),
                @Index(
                        name = "idx_release_history_nominee",
                        columnList = "nominee_id"
                ),
                @Index(
                        name = "idx_release_history_document",
                        columnList = "document_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyReleaseHistory {

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
                    name = "fk_release_history_request"
            )
    )
    private EmergencyRequest emergencyRequest;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "nominee_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_release_history_nominee"
            )
    )
    private Nominee nominee;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_release_history_document"
            )
    )
    private Document document;

    @Column(
            name = "can_view",
            nullable = false
    )
    @Builder.Default
    private Boolean canView = true;

    @Column(
            name = "can_download",
            nullable = false
    )
    @Builder.Default
    private Boolean canDownload = false;

    @Column(
            name = "released_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime releasedAt;

    @Column(
            name = "access_expires_at"
    )
    private LocalDateTime accessExpiresAt;

    @Column(
            name = "access_revoked",
            nullable = false
    )
    @Builder.Default
    private Boolean accessRevoked = false;

    @Column(
            name = "revoked_at"
    )
    private LocalDateTime revokedAt;

    @Column(
            name = "revocation_reason",
            length = 500
    )
    private String revocationReason;

    @Column(
            name = "download_count",
            nullable = false
    )
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(
            name = "last_accessed_at"
    )
    private LocalDateTime lastAccessedAt;

    @PrePersist
    protected void onCreate() {

        releasedAt = LocalDateTime.now();

        if (canView == null) {
            canView = true;
        }

        if (canDownload == null) {
            canDownload = false;
        }

        if (accessRevoked == null) {
            accessRevoked = false;
        }

        if (downloadCount == null) {
            downloadCount = 0;
        }

        revocationReason =
                normalizeNullable(revocationReason);
    }

    @PreUpdate
    protected void onUpdate() {

        revocationReason =
                normalizeNullable(revocationReason);
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