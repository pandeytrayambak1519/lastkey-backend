package com.lastkey.backend.notification.mapper;

import com.lastkey.backend.notification.dto.response.NotificationResponse;
import com.lastkey.backend.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(
            Notification notification
    ) {

        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .priority(notification.getPriority())
                .read(notification.getRead())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .resourceType(
                        notification.getResourceType()
                )
                .resourceId(
                        notification.getResourceId()
                )
                .metadata(notification.getMetadata())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}