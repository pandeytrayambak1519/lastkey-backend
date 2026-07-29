package com.lastkey.backend.notification.service;

import com.lastkey.backend.notification.dto.response.NotificationResponse;
import com.lastkey.backend.notification.enums.NotificationPriority;
import com.lastkey.backend.notification.enums.NotificationType;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    NotificationResponse createNotification(
            User recipient,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            String actionUrl,
            String resourceType,
            String resourceId,
            String metadata
    );

    Page<NotificationResponse> getMyNotifications(
            Pageable pageable
    );

    NotificationResponse getMyNotificationById(
            UUID notificationId
    );

    Page<NotificationResponse> getMyUnreadNotifications(
            Pageable pageable
    );

    Page<NotificationResponse> getMyNotificationsByType(
            NotificationType type,
            Pageable pageable
    );

    long getMyUnreadCount();

    NotificationResponse markAsRead(
            UUID notificationId
    );

    NotificationResponse markAsUnread(
            UUID notificationId
    );

    int markAllAsRead();

    void deleteNotification(
            UUID notificationId
    );

    int clearReadNotifications();

    int clearAllNotifications();
}