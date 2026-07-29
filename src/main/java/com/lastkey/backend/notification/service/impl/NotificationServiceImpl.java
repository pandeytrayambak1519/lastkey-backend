package com.lastkey.backend.notification.service.impl;

import com.lastkey.backend.notification.dto.response.NotificationResponse;
import com.lastkey.backend.notification.entity.Notification;
import com.lastkey.backend.notification.enums.NotificationPriority;
import com.lastkey.backend.notification.enums.NotificationType;
import com.lastkey.backend.notification.exception.NotificationNotFoundException;
import com.lastkey.backend.notification.mapper.NotificationMapper;
import com.lastkey.backend.notification.repository.NotificationRepository;
import com.lastkey.backend.notification.service.NotificationService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.exception.UserNotFoundException;
import com.lastkey.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository
            notificationRepository;

    private final UserRepository
            userRepository;

    private final NotificationMapper
            notificationMapper;

    @Override
    @Transactional
    public NotificationResponse createNotification(
            User recipient,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            String actionUrl,
            String resourceType,
            String resourceId,
            String metadata
    ) {

        if (recipient == null
                || recipient.getId() == null) {

            throw new IllegalArgumentException(
                    "Notification recipient is required."
            );
        }

        if (title == null
                || title.isBlank()) {

            throw new IllegalArgumentException(
                    "Notification title is required."
            );
        }

        if (message == null
                || message.isBlank()) {

            throw new IllegalArgumentException(
                    "Notification message is required."
            );
        }

        Notification notification =
                Notification.builder()
                        .recipient(recipient)
                        .title(title.trim())
                        .message(message.trim())
                        .type(
                                type != null
                                        ? type
                                        : NotificationType.SYSTEM
                        )
                        .priority(
                                priority != null
                                        ? priority
                                        : NotificationPriority.MEDIUM
                        )
                        .read(false)
                        .readAt(null)
                        .actionUrl(
                                normalizeNullable(
                                        actionUrl
                                )
                        )
                        .resourceType(
                                normalizeUppercase(
                                        resourceType
                                )
                        )
                        .resourceId(
                                normalizeNullable(
                                        resourceId
                                )
                        )
                        .metadata(
                                normalizeNullable(
                                        metadata
                                )
                        )
                        .active(true)
                        .build();

        Notification savedNotification =
                notificationRepository.save(
                        notification
                );

        return notificationMapper.toResponse(
                savedNotification
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse>
    getMyNotifications(
            Pageable pageable
    ) {

        User currentUser =
                getCurrentUser();

        return notificationRepository
                .findByRecipientAndActiveTrueOrderByCreatedAtDesc(
                        currentUser,
                        pageable
                )
                .map(
                        notificationMapper::toResponse
                );
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse
    getMyNotificationById(
            UUID notificationId
    ) {

        User currentUser =
                getCurrentUser();

        Notification notification =
                findOwnedActiveNotification(
                        notificationId,
                        currentUser
                );

        return notificationMapper.toResponse(
                notification
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse>
    getMyUnreadNotifications(
            Pageable pageable
    ) {

        User currentUser =
                getCurrentUser();

        return notificationRepository
                .findByRecipientAndReadFalseAndActiveTrueOrderByCreatedAtDesc(
                        currentUser,
                        pageable
                )
                .map(
                        notificationMapper::toResponse
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse>
    getMyNotificationsByType(
            NotificationType type,
            Pageable pageable
    ) {

        if (type == null) {

            throw new IllegalArgumentException(
                    "Notification type is required."
            );
        }

        User currentUser =
                getCurrentUser();

        return notificationRepository
                .findByRecipientAndTypeAndActiveTrueOrderByCreatedAtDesc(
                        currentUser,
                        type,
                        pageable
                )
                .map(
                        notificationMapper::toResponse
                );
    }

    @Override
    @Transactional(readOnly = true)
    public long getMyUnreadCount() {

        User currentUser =
                getCurrentUser();

        return notificationRepository
                .countByRecipientAndReadFalseAndActiveTrue(
                        currentUser
                );
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(
            UUID notificationId
    ) {

        User currentUser =
                getCurrentUser();

        Notification notification =
                findOwnedActiveNotification(
                        notificationId,
                        currentUser
                );

        if (!Boolean.TRUE.equals(
                notification.getRead()
        )) {

            LocalDateTime now =
                    LocalDateTime.now();

            notification.setRead(true);
            notification.setReadAt(now);

            notification =
                    notificationRepository.save(
                            notification
                    );
        }

        return notificationMapper.toResponse(
                notification
        );
    }

    @Override
    @Transactional
    public NotificationResponse markAsUnread(
            UUID notificationId
    ) {

        User currentUser =
                getCurrentUser();

        Notification notification =
                findOwnedActiveNotification(
                        notificationId,
                        currentUser
                );

        if (Boolean.TRUE.equals(
                notification.getRead()
        )) {

            notification.setRead(false);
            notification.setReadAt(null);

            notification =
                    notificationRepository.save(
                            notification
                    );
        }

        return notificationMapper.toResponse(
                notification
        );
    }

    @Override
    @Transactional
    public int markAllAsRead() {

        User currentUser =
                getCurrentUser();

        return notificationRepository
                .markAllAsRead(
                        currentUser,
                        LocalDateTime.now()
                );
    }

    @Override
    @Transactional
    public void deleteNotification(
            UUID notificationId
    ) {

        validateNotificationId(
                notificationId
        );

        User currentUser =
                getCurrentUser();

        int updatedRows =
                notificationRepository
                        .softDeleteByIdAndRecipient(
                                notificationId,
                                currentUser,
                                LocalDateTime.now()
                        );

        if (updatedRows == 0) {

            throw new NotificationNotFoundException(
                    "Notification not found with ID: "
                            + notificationId
            );
        }
    }

    @Override
    @Transactional
    public int clearReadNotifications() {

        User currentUser =
                getCurrentUser();

        return notificationRepository
                .softDeleteReadNotifications(
                        currentUser,
                        LocalDateTime.now()
                );
    }

    @Override
    @Transactional
    public int clearAllNotifications() {

        User currentUser =
                getCurrentUser();

        return notificationRepository
                .softDeleteAllNotifications(
                        currentUser,
                        LocalDateTime.now()
                );
    }

    private Notification findOwnedActiveNotification(
            UUID notificationId,
            User currentUser
    ) {

        validateNotificationId(
                notificationId
        );

        return notificationRepository
                .findByIdAndRecipientAndActiveTrue(
                        notificationId,
                        currentUser
                )
                .orElseThrow(() ->
                        new NotificationNotFoundException(
                                "Notification not found with ID: "
                                        + notificationId
                        )
                );
    }

    private void validateNotificationId(
            UUID notificationId
    ) {

        if (notificationId == null) {

            throw new IllegalArgumentException(
                    "Notification ID is required."
            );
        }
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(
                authentication.getName()
        )) {

            throw new IllegalStateException(
                    "Authenticated user is required."
            );
        }

        String email =
                authentication
                        .getName()
                        .trim()
                        .toLowerCase();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Authenticated user not found."
                        )
                );
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private String normalizeUppercase(
            String value
    ) {

        String normalized =
                normalizeNullable(value);

        return normalized != null
                ? normalized.toUpperCase()
                : null;
    }
}