package com.lastkey.backend.dashboard.dto;

import com.lastkey.backend.notification.enums.NotificationPriority;
import com.lastkey.backend.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentNotificationResponse {

    private UUID id;

    private String title;

    private String message;

    private NotificationType type;

    private NotificationPriority priority;

    private Boolean read;

    private String actionUrl;

    private LocalDateTime createdAt;
}