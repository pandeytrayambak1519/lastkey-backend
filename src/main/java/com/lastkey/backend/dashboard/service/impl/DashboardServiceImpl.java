package com.lastkey.backend.dashboard.service.impl;

import com.lastkey.backend.category.repository.CategoryRepository;
import com.lastkey.backend.dashboard.dto.DashboardResponse;
import com.lastkey.backend.dashboard.dto.DashboardStatsResponse;
import com.lastkey.backend.dashboard.dto.RecentDocumentResponse;
import com.lastkey.backend.dashboard.dto.RecentNotificationResponse;
import com.lastkey.backend.dashboard.service.DashboardService;
import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.document.repository.DocumentRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestRepository;
import com.lastkey.backend.nominee.repository.NomineeRepository;
import com.lastkey.backend.notification.entity.Notification;
import com.lastkey.backend.notification.repository.NotificationRepository;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.exception.UserNotFoundException;
import com.lastkey.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardServiceImpl
        implements DashboardService {

    private final UserRepository userRepository;

    private final DocumentRepository documentRepository;

    private final CategoryRepository categoryRepository;

    private final NomineeRepository nomineeRepository;

    private final EmergencyRequestRepository
            emergencyRequestRepository;

    private final NotificationRepository
            notificationRepository;

    public DashboardServiceImpl(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            CategoryRepository categoryRepository,
            NomineeRepository nomineeRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            NotificationRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.nomineeRepository = nomineeRepository;
        this.emergencyRequestRepository =
                emergencyRequestRepository;
        this.notificationRepository =
                notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getCurrentUserDashboard() {

        User currentUser = getAuthenticatedUser();

        LocalDate currentDate = LocalDate.now();

        LocalDate expiryEndDate =
                currentDate.plusDays(30);

        DashboardStatsResponse statistics =
                buildDashboardStatistics(
                        currentUser,
                        currentDate,
                        expiryEndDate
                );

        List<RecentDocumentResponse> recentDocuments =
                documentRepository
                        .findTop5ByOwnerAndStatusOrderByCreatedAtDesc(
                                currentUser,
                                DocumentStatus.ACTIVE
                        )
                        .stream()
                        .map(this::mapRecentDocument)
                        .toList();

        List<RecentNotificationResponse>
                recentNotifications =
                notificationRepository
                        .findTop5ByRecipientAndActiveTrueOrderByCreatedAtDesc(
                                currentUser
                        )
                        .stream()
                        .map(this::mapRecentNotification)
                        .toList();

        String fullName =
                buildFullName(currentUser);

        return DashboardResponse.builder()
                .userName(fullName)
                .email(currentUser.getEmail())
                .statistics(statistics)
                .recentDocuments(recentDocuments)
                .recentNotifications(recentNotifications)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private DashboardStatsResponse
    buildDashboardStatistics(
            User currentUser,
            LocalDate currentDate,
            LocalDate expiryEndDate
    ) {

        long totalDocuments =
                documentRepository
                        .countByOwnerAndStatus(
                                currentUser,
                                DocumentStatus.ACTIVE
                        );

        long favoriteDocuments =
                documentRepository
                        .countByOwnerAndFavoriteTrueAndStatus(
                                currentUser,
                                DocumentStatus.ACTIVE
                        );

        long archivedDocuments =
                documentRepository
                        .countByOwnerAndArchivedTrueAndStatus(
                                currentUser,
                                DocumentStatus.ACTIVE
                        );

        long totalCategories =
                categoryRepository
                        .countVisibleCategories(
                                currentUser
                        );

        long totalNominees =
                nomineeRepository
                        .countByOwnerAndActiveTrue(
                                currentUser
                        );

        long activeEmergencies =
                emergencyRequestRepository
                        .countByOwnerAndActiveTrue(
                                currentUser
                        );

        long unreadNotifications =
                notificationRepository
                        .countByRecipientAndReadFalseAndActiveTrue(
                                currentUser
                        );

        long expiringDocuments =
                documentRepository
                        .countByOwnerAndStatusAndExpiryDateBetween(
                                currentUser,
                                DocumentStatus.ACTIVE,
                                currentDate,
                                expiryEndDate
                        );

        return DashboardStatsResponse.builder()
                .totalDocuments(totalDocuments)
                .favoriteDocuments(favoriteDocuments)
                .archivedDocuments(archivedDocuments)
                .totalCategories(totalCategories)
                .totalNominees(totalNominees)
                .activeEmergencies(activeEmergencies)
                .unreadNotifications(unreadNotifications)
                .expiringDocuments(expiringDocuments)
                .build();
    }

    private RecentDocumentResponse mapRecentDocument(
            Document document
    ) {

        String categoryName = null;

        if (document.getCategory() != null) {
            categoryName =
                    document.getCategory().getName();
        }

        return RecentDocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileType(document.getFileType())
                .categoryName(categoryName)
                .favorite(document.getFavorite())
                .archived(document.getArchived())
                .expiryDate(document.getExpiryDate())
                .createdAt(document.getCreatedAt())
                .build();
    }

    private RecentNotificationResponse
    mapRecentNotification(
            Notification notification
    ) {

        return RecentNotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .priority(notification.getPriority())
                .read(notification.getRead())
                .actionUrl(notification.getActionUrl())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String buildFullName(
            User user
    ) {

        String firstName =
                user.getFirstName() == null
                        ? ""
                        : user.getFirstName().trim();

        String lastName =
                user.getLastName() == null
                        ? ""
                        : user.getLastName().trim();

        String fullName =
                (firstName + " " + lastName).trim();

        return fullName.isBlank()
                ? "User"
                : fullName;
    }

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new UserNotFoundException(
                    "Authenticated user information is not available"
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found with email: "
                                        + email
                        )
                );
    }
}