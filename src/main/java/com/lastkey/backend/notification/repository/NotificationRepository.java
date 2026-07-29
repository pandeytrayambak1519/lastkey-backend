package com.lastkey.backend.notification.repository;

import com.lastkey.backend.notification.entity.Notification;
import com.lastkey.backend.notification.enums.NotificationType;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    Page<Notification>
    findByRecipientAndActiveTrueOrderByCreatedAtDesc(
            User recipient,
            Pageable pageable
    );

    Page<Notification>
    findByRecipientAndReadFalseAndActiveTrueOrderByCreatedAtDesc(
            User recipient,
            Pageable pageable
    );

    List<Notification>
    findByRecipientAndReadFalseAndActiveTrueOrderByCreatedAtDesc(
            User recipient
    );

    long countByRecipientAndReadFalseAndActiveTrue(
            User recipient
    );

    boolean existsByIdAndRecipientAndActiveTrue(
            UUID notificationId,
            User recipient
    );

    Optional<Notification>
    findByIdAndRecipientAndActiveTrue(
            UUID notificationId,
            User recipient
    );

    Page<Notification>
    findByRecipientAndTypeAndActiveTrueOrderByCreatedAtDesc(
            User recipient,
            NotificationType type,
            Pageable pageable
    );

    List<Notification>
    findByActiveTrueAndCreatedAtBefore(
            LocalDateTime createdBefore
    );

    List<Notification>
    findTop5ByRecipientAndActiveTrueOrderByCreatedAtDesc(
            User recipient
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update Notification notification
               set notification.read = true,
                   notification.readAt = :readAt,
                   notification.updatedAt = :readAt
             where notification.recipient = :recipient
               and notification.read = false
               and notification.active = true
            """)
    int markAllAsRead(
            @Param("recipient")
            User recipient,

            @Param("readAt")
            LocalDateTime readAt
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update Notification notification
               set notification.active = false,
                   notification.updatedAt = :updatedAt
             where notification.id = :notificationId
               and notification.recipient = :recipient
               and notification.active = true
            """)
    int softDeleteByIdAndRecipient(
            @Param("notificationId")
            UUID notificationId,

            @Param("recipient")
            User recipient,

            @Param("updatedAt")
            LocalDateTime updatedAt
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update Notification notification
               set notification.active = false,
                   notification.updatedAt = :updatedAt
             where notification.recipient = :recipient
               and notification.read = true
               and notification.active = true
            """)
    int softDeleteReadNotifications(
            @Param("recipient")
            User recipient,

            @Param("updatedAt")
            LocalDateTime updatedAt
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            update Notification notification
               set notification.active = false,
                   notification.updatedAt = :updatedAt
             where notification.recipient = :recipient
               and notification.active = true
            """)
    int softDeleteAllNotifications(
            @Param("recipient")
            User recipient,

            @Param("updatedAt")
            LocalDateTime updatedAt
    );
}