package com.lastkey.backend.notification.dto.response;

import com.lastkey.backend.notification.enums.NotificationPriority;
import com.lastkey.backend.notification.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;

    private String title;

    private String message;

    private NotificationType type;

    private NotificationPriority priority;

    private Boolean read;

    private LocalDateTime readAt;

    private String actionUrl;

    private String resourceType;

    private String resourceId;

    private String metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}