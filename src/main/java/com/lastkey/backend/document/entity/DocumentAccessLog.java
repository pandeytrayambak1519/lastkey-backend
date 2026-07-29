package com.lastkey.backend.document.entity;

import com.lastkey.backend.document.enums.AccessAction;
import com.lastkey.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "document_access_logs",
        indexes = {
                @Index(
                        name = "idx_access_log_document",
                        columnList = "document_id"
                ),
                @Index(
                        name = "idx_access_log_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_access_log_action",
                        columnList = "action"
                ),
                @Index(
                        name = "idx_access_log_accessed_at",
                        columnList = "accessed_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_access_log_document"
            )
    )
    private Document document;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_access_log_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private AccessAction action;

    @Column(
            name = "ip_address",
            length = 45
    )
    private String ipAddress;

    @Column(
            name = "user_agent",
            length = 500
    )
    private String userAgent;

    @Builder.Default
    @Column(
            name = "successful",
            nullable = false
    )
    private Boolean successful = true;

    @Column(
            name = "failure_reason",
            length = 500
    )
    private String failureReason;

    @Column(
            name = "accessed_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime accessedAt;

    @PrePersist
    protected void onCreate() {

        if (this.accessedAt == null) {
            this.accessedAt = LocalDateTime.now();
        }

        if (this.successful == null) {
            this.successful = true;
        }

        normalizeFields();
    }

    private void normalizeFields() {

        if (this.ipAddress != null) {
            this.ipAddress = normalizeNullable(
                    this.ipAddress
            );
        }

        if (this.userAgent != null) {
            this.userAgent = normalizeNullable(
                    this.userAgent
            );
        }

        if (this.failureReason != null) {
            this.failureReason = normalizeNullable(
                    this.failureReason
            );
        }
    }

    private String normalizeNullable(
            String value
    ) {

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}