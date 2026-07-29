package com.lastkey.backend.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(
                        name = "idx_audit_actor_email",
                        columnList = "actor_email"
                ),
                @Index(
                        name = "idx_audit_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_audit_resource",
                        columnList = "resource_type, resource_id"
                ),
                @Index(
                        name = "idx_audit_http_status",
                        columnList = "http_status"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Authenticated user's email.
     *
     * For unauthenticated APIs such as login/register,
     * this value will be ANONYMOUS.
     */
    @Column(
            name = "actor_email",
            nullable = false,
            length = 255
    )
    private String actorEmail;

    /*
     * USER, ADMIN, SYSTEM or ANONYMOUS.
     */
    @Column(
            name = "actor_type",
            nullable = false,
            length = 50
    )
    private String actorType;

    /*
     * Example:
     * GET_API_V1_DOCUMENTS
     * POST_API_V1_AUTH_LOGIN
     */
    @Column(
            name = "action",
            nullable = false,
            length = 255
    )
    private String action;

    /*
     * Example:
     * DOCUMENT, EMERGENCY, NOMINEE, AUTH.
     */
    @Column(
            name = "resource_type",
            nullable = false,
            length = 100
    )
    private String resourceType;

    /*
     * Resource UUID or path identifier when available.
     */
    @Column(
            name = "resource_id",
            length = 255
    )
    private String resourceId;

    @Column(
            name = "http_method",
            nullable = false,
            length = 20
    )
    private String httpMethod;

    @Column(
            name = "endpoint",
            nullable = false,
            length = 500
    )
    private String endpoint;

    @Column(
            name = "http_status",
            nullable = false
    )
    private Integer httpStatus;

    @Column(
            name = "successful",
            nullable = false
    )
    private Boolean successful;

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
            name = "execution_time_ms"
    )
    private Long executionTimeMs;

    @Column(
            name = "failure_message",
            columnDefinition = "TEXT"
    )
    private String failureMessage;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (successful == null) {
            successful = false;
        }

        if (actorEmail == null || actorEmail.isBlank()) {
            actorEmail = "ANONYMOUS";
        }

        if (actorType == null || actorType.isBlank()) {
            actorType = "ANONYMOUS";
        }
    }
}