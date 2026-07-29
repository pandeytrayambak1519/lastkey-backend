package com.lastkey.backend.notification.entity;

import com.lastkey.backend.notification.enums.NotificationPriority;
import com.lastkey.backend.notification.enums.NotificationType;
import com.lastkey.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notification_recipient",
                        columnList = "recipient_id"
                ),
                @Index(
                        name = "idx_notification_recipient_read",
                        columnList = "recipient_id, is_read"
                ),
                @Index(
                        name = "idx_notification_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_notification_type",
                        columnList = "notification_type"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recipient_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_notification_recipient"
            )
    )
    private User recipient;

    @Column(
            name = "title",
            nullable = false,
            length = 200
    )
    private String title;

    @Column(
            name = "message",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_type",
            nullable = false,
            length = 100
    )
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "priority",
            nullable = false,
            length = 50
    )
    private NotificationPriority priority;

    @Builder.Default
    @Column(
            name = "is_read",
            nullable = false
    )
    private Boolean read = false;

    @Column(
            name = "read_at"
    )
    private LocalDateTime readAt;

    /*
     * Frontend can redirect the user to this route.
     *
     * Examples:
     * /documents/{id}
     * /emergencies/{id}
     * /nominees/{id}
     */
    @Column(
            name = "action_url",
            length = 500
    )
    private String actionUrl;

    /*
     * Optional related resource information.
     *
     * Example:
     * resourceType = DOCUMENT
     * resourceId = document UUID
     */
    @Column(
            name = "resource_type",
            length = 100
    )
    private String resourceType;

    @Column(
            name = "resource_id",
            length = 255
    )
    private String resourceId;

    /*
     * Optional JSON or text information.
     *
     * Do not store passwords, JWT tokens, encryption keys,
     * document contents or other sensitive information here.
     */
    @Column(
            name = "metadata",
            columnDefinition = "TEXT"
    )
    private String metadata;

    @Builder.Default
    @Column(
            name = "active",
            nullable = false
    )
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

    @PrePersist
    protected void onCreate() {

        LocalDateTime currentTime =
                LocalDateTime.now();

        if (createdAt == null) {
            createdAt = currentTime;
        }

        if (updatedAt == null) {
            updatedAt = currentTime;
        }

        if (read == null) {
            read = false;
        }

        if (active == null) {
            active = true;
        }

        if (priority == null) {
            priority = NotificationPriority.MEDIUM;
        }

        if (type == null) {
            type = NotificationType.SYSTEM;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}