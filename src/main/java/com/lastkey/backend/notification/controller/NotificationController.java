package com.lastkey.backend.notification.controller;

import com.lastkey.backend.notification.dto.response.NotificationActionResponse;
import com.lastkey.backend.notification.dto.response.NotificationResponse;
import com.lastkey.backend.notification.dto.response.UnreadNotificationCountResponse;
import com.lastkey.backend.notification.enums.NotificationType;
import com.lastkey.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(
        name = "Notification APIs",
        description = "APIs for reading and managing notifications of the authenticated user."
)
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final NotificationService notificationService;

    /*
     * =========================================================
     * GET ALL NOTIFICATIONS
     * =========================================================
     */

    @Operation(
            summary = "Get my notifications",
            description = """
                    Returns all active notifications belonging to the
                    currently authenticated user in paginated form.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications retrieved successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            )
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>>
    getMyNotifications(

            @Parameter(
                    description = "Zero-based page number.",
                    example = "0",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(
                    description = "Number of notifications per page. Maximum value is 100.",
                    example = "20",
                    in = ParameterIn.QUERY
            )
            @RequestParam(
                    defaultValue = "" + DEFAULT_PAGE_SIZE
            )
            int size
    ) {

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        return ResponseEntity.ok(
                notificationService
                        .getMyNotifications(
                                pageable
                        )
        );
    }

    /*
     * =========================================================
     * GET ONE NOTIFICATION
     * =========================================================
     */

    @Operation(
            summary = "Get notification by ID",
            description = """
                    Returns one active notification when it belongs to
                    the currently authenticated user.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification retrieved successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification was not found."
            )
    })
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse>
    getMyNotificationById(

            @Parameter(
                    description = "Unique notification ID.",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable
            UUID notificationId
    ) {

        return ResponseEntity.ok(
                notificationService
                        .getMyNotificationById(
                                notificationId
                        )
        );
    }

    /*
     * =========================================================
     * GET UNREAD NOTIFICATIONS
     * =========================================================
     */

    @Operation(
            summary = "Get unread notifications",
            description = """
                    Returns unread and active notifications belonging
                    to the currently authenticated user.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Unread notifications retrieved successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            )
    })
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>>
    getMyUnreadNotifications(

            @Parameter(
                    description = "Zero-based page number.",
                    example = "0",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(
                    description = "Number of notifications per page. Maximum value is 100.",
                    example = "20",
                    in = ParameterIn.QUERY
            )
            @RequestParam(
                    defaultValue = "" + DEFAULT_PAGE_SIZE
            )
            int size
    ) {

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        return ResponseEntity.ok(
                notificationService
                        .getMyUnreadNotifications(
                                pageable
                        )
        );
    }

    /*
     * =========================================================
     * GET UNREAD COUNT
     * =========================================================
     */

    @Operation(
            summary = "Get unread notification count",
            description = """
                    Returns the total number of unread and active
                    notifications of the authenticated user.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Unread notification count retrieved successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            )
    })
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse>
    getMyUnreadCount() {

        long unreadCount =
                notificationService
                        .getMyUnreadCount();

        UnreadNotificationCountResponse response =
                UnreadNotificationCountResponse
                        .builder()
                        .unreadCount(
                                unreadCount
                        )
                        .build();

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * =========================================================
     * GET NOTIFICATIONS BY TYPE
     * =========================================================
     */

    @Operation(
            summary = "Get notifications by type",
            description = """
                    Returns active notifications of the authenticated
                    user filtered by notification type.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications retrieved successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid notification type or pagination parameters."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            )
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<Page<NotificationResponse>>
    getMyNotificationsByType(

            @Parameter(
                    description = "Notification type.",
                    required = true,
                    example = "DOCUMENT_UPLOADED"
            )
            @PathVariable
            NotificationType type,

            @Parameter(
                    description = "Zero-based page number.",
                    example = "0",
                    in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(
                    description = "Number of notifications per page. Maximum value is 100.",
                    example = "20",
                    in = ParameterIn.QUERY
            )
            @RequestParam(
                    defaultValue = "" + DEFAULT_PAGE_SIZE
            )
            int size
    ) {

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        return ResponseEntity.ok(
                notificationService
                        .getMyNotificationsByType(
                                type,
                                pageable
                        )
        );
    }

    /*
     * =========================================================
     * MARK ONE NOTIFICATION AS READ
     * =========================================================
     */

    @Operation(
            summary = "Mark notification as read",
            description = """
                    Marks one notification belonging to the authenticated
                    user as read and records the read timestamp.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification marked as read successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification was not found."
            )
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse>
    markAsRead(

            @Parameter(
                    description = "Unique notification ID.",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable
            UUID notificationId
    ) {

        return ResponseEntity.ok(
                notificationService
                        .markAsRead(
                                notificationId
                        )
        );
    }

    /*
     * =========================================================
     * MARK ONE NOTIFICATION AS UNREAD
     * =========================================================
     */

    @Operation(
            summary = "Mark notification as unread",
            description = """
                    Marks one notification belonging to the authenticated
                    user as unread and removes its read timestamp.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification marked as unread successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification was not found."
            )
    })
    @PatchMapping("/{notificationId}/unread")
    public ResponseEntity<NotificationResponse>
    markAsUnread(

            @Parameter(
                    description = "Unique notification ID.",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable
            UUID notificationId
    ) {

        return ResponseEntity.ok(
                notificationService
                        .markAsUnread(
                                notificationId
                        )
        );
    }

    /*
     * =========================================================
     * MARK ALL NOTIFICATIONS AS READ
     * =========================================================
     */

    @Operation(
            summary = "Mark all notifications as read",
            description = """
                    Marks every unread and active notification of the
                    authenticated user as read.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications marked as read successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            )
    })
    @PatchMapping("/read-all")
    public ResponseEntity<NotificationActionResponse>
    markAllAsRead() {

        int updatedCount =
                notificationService
                        .markAllAsRead();

        NotificationActionResponse response =
                NotificationActionResponse
                        .builder()
                        .message(
                                "All notifications marked as read."
                        )
                        .affectedCount(
                                updatedCount
                        )
                        .build();

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * =========================================================
     * DELETE ONE NOTIFICATION
     * =========================================================
     */

    @Operation(
            summary = "Delete notification",
            description = """
                    Soft deletes one notification when it belongs to
                    the currently authenticated user.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification deleted successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Notification was not found."
            )
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<NotificationActionResponse>
    deleteNotification(

            @Parameter(
                    description = "Unique notification ID.",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable
            UUID notificationId
    ) {

        notificationService
                .deleteNotification(
                        notificationId
                );

        NotificationActionResponse response =
                NotificationActionResponse
                        .builder()
                        .message(
                                "Notification deleted successfully."
                        )
                        .affectedCount(1)
                        .build();

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * =========================================================
     * CLEAR READ NOTIFICATIONS
     * =========================================================
     */

    @Operation(
            summary = "Clear read notifications",
            description = """
                    Soft deletes all read and active notifications
                    belonging to the authenticated user.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Read notifications cleared successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            )
    })
    @DeleteMapping("/clear-read")
    public ResponseEntity<NotificationActionResponse>
    clearReadNotifications() {

        int deletedCount =
                notificationService
                        .clearReadNotifications();

        NotificationActionResponse response =
                NotificationActionResponse
                        .builder()
                        .message(
                                "Read notifications cleared successfully."
                        )
                        .affectedCount(
                                deletedCount
                        )
                        .build();

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * =========================================================
     * CLEAR ALL NOTIFICATIONS
     * =========================================================
     */

    @Operation(
            summary = "Clear all notifications",
            description = """
                    Soft deletes every active notification belonging
                    to the currently authenticated user.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "All notifications cleared successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required."
            )
    })
    @DeleteMapping("/clear-all")
    public ResponseEntity<NotificationActionResponse>
    clearAllNotifications() {

        int deletedCount =
                notificationService
                        .clearAllNotifications();

        NotificationActionResponse response =
                NotificationActionResponse
                        .builder()
                        .message(
                                "All notifications cleared successfully."
                        )
                        .affectedCount(
                                deletedCount
                        )
                        .build();

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * =========================================================
     * PAGEABLE HELPER
     * =========================================================
     */

    private Pageable createPageable(
            int page,
            int size
    ) {

        if (page < 0) {

            throw new IllegalArgumentException(
                    "Page number cannot be negative."
            );
        }

        if (size < 1) {

            throw new IllegalArgumentException(
                    "Page size must be greater than zero."
            );
        }

        int validatedSize =
                Math.min(
                        size,
                        MAXIMUM_PAGE_SIZE
                );

        return PageRequest.of(
                page,
                validatedSize,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }
}