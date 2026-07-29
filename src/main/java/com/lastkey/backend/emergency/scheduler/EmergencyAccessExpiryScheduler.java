package com.lastkey.backend.emergency.scheduler;

import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.entity.EmergencyRequestLog;
import com.lastkey.backend.emergency.enums.EmergencyLogAction;
import com.lastkey.backend.emergency.repository.EmergencyReleaseHistoryRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestLogRepository;
import com.lastkey.backend.notification.service.NotificationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmergencyAccessExpiryScheduler {

    private final EmergencyReleaseHistoryRepository
            emergencyReleaseHistoryRepository;

    private final EmergencyRequestLogRepository
            emergencyRequestLogRepository;

    private final NotificationEventService
            notificationEventService;

    /*
     * Runs once every hour and disables access to documents
     * whose nominee-access expiry time has passed.
     */
    @Scheduled(
            fixedDelayString =
                    "${lastkey.emergency.access-expiry-delay:3600000}",
            initialDelayString =
                    "${lastkey.emergency.access-expiry-initial-delay:120000}"
    )
    @Transactional
    public void revokeExpiredDocumentAccess() {

        LocalDateTime currentTime =
                LocalDateTime.now();

        log.debug(
                "Starting expired emergency document-access scheduler at {}",
                currentTime
        );

        List<EmergencyReleaseHistory> expiredAccessRecords =
                emergencyReleaseHistoryRepository
                        .findByAccessRevokedFalseAndAccessExpiresAtLessThanEqual(
                                currentTime
                        );

        if (expiredAccessRecords.isEmpty()) {

            log.debug(
                    "No expired emergency document-access records found"
            );

            return;
        }

        int revokedCount = 0;
        int failedCount = 0;

        for (EmergencyReleaseHistory releaseHistory
                : expiredAccessRecords) {

            try {

                revokeAccess(
                        releaseHistory,
                        currentTime
                );

                revokedCount++;

            } catch (Exception exception) {

                failedCount++;

                log.error(
                        "Failed to revoke expired access for " +
                                "release-history ID: {}",
                        releaseHistory.getId(),
                        exception
                );
            }
        }

        log.info(
                "Emergency access-expiry scheduler completed. " +
                        "Revoked: {}, Failed: {}",
                revokedCount,
                failedCount
        );
    }

    /*
     * ---------------------------------------------------------
     * REVOKE ONE RELEASED DOCUMENT ACCESS
     * ---------------------------------------------------------
     */

    private void revokeAccess(
            EmergencyReleaseHistory releaseHistory,
            LocalDateTime revokedAt
    ) {

        /*
         * Additional protection in case an already-revoked
         * record reaches this method.
         */
        if (Boolean.TRUE.equals(
                releaseHistory.getAccessRevoked()
        )) {

            log.debug(
                    "Release-history ID {} is already revoked",
                    releaseHistory.getId()
            );

            return;
        }

        releaseHistory.setAccessRevoked(
                true
        );

        releaseHistory.setCanView(
                false
        );

        releaseHistory.setCanDownload(
                false
        );

        releaseHistory.setRevokedAt(
                revokedAt
        );

        releaseHistory.setRevocationReason(
                "Document access period expired automatically"
        );

        emergencyReleaseHistoryRepository.save(
                releaseHistory
        );

        createAuditLog(
                releaseHistory
        );

        sendAccessExpiredNotification(
                releaseHistory
        );

        log.info(
                "Expired nominee access revoked for release-history ID: {}",
                releaseHistory.getId()
        );
    }

    /*
     * ---------------------------------------------------------
     * SEND ACCESS-EXPIRED NOTIFICATION
     * ---------------------------------------------------------
     */

    private void sendAccessExpiredNotification(
            EmergencyReleaseHistory releaseHistory
    ) {

        try {

            notificationEventService.accessExpired(
                    releaseHistory
            );

        } catch (Exception exception) {

            /*
             * Notification failure must not restore or prevent
             * access revocation.
             */
            log.error(
                    "Access was revoked successfully, but expiry " +
                            "notification failed for release-history ID: {}",
                    releaseHistory.getId(),
                    exception
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * CREATE AUDIT LOG
     * ---------------------------------------------------------
     */

    private void createAuditLog(
            EmergencyReleaseHistory releaseHistory
    ) {

        EmergencyRequest emergencyRequest =
                releaseHistory.getEmergencyRequest();

        if (emergencyRequest == null) {

            log.warn(
                    "Audit log was not created because emergency request " +
                            "is missing for release-history ID: {}",
                    releaseHistory.getId()
            );

            return;
        }

        String documentTitle =
                releaseHistory.getDocument() != null
                        && releaseHistory
                        .getDocument()
                        .getTitle() != null
                        ? releaseHistory
                        .getDocument()
                        .getTitle()
                        : "Unknown document";

        EmergencyRequestLog logEntry =
                EmergencyRequestLog.builder()
                        .emergencyRequest(
                                emergencyRequest
                        )
                        .action(
                                EmergencyLogAction.ACCESS_REVOKED
                        )
                        .previousStatus(
                                emergencyRequest.getStatus()
                        )
                        .newStatus(
                                emergencyRequest.getStatus()
                        )
                        .performedBy("SYSTEM")
                        .performedByType("SYSTEM")
                        .message(
                                "Expired nominee access automatically " +
                                        "revoked for document: "
                                        + documentTitle
                        )
                        .build();

        emergencyRequestLogRepository.save(
                logEntry
        );
    }
}