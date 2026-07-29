package com.lastkey.backend.notification.scheduler;

import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.repository.EmergencyReleaseHistoryRepository;
import com.lastkey.backend.notification.service.NotificationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessExpiryScheduler {

    private static final String AUTOMATIC_EXPIRY_REASON =
            "Temporary emergency document access expired automatically";

    private final EmergencyReleaseHistoryRepository
            emergencyReleaseHistoryRepository;

    private final NotificationEventService
            notificationEventService;

    /*
     * Runs every hour by default.
     *
     * The cron expression can be overridden with:
     *
     * lastkey.scheduler.access-expiry.cron
     */
    @Scheduled(
            cron = "${lastkey.scheduler.access-expiry.cron:0 0 * * * *}"
    )
    @Transactional
    public void revokeExpiredDocumentAccess() {

        LocalDateTime now =
                LocalDateTime.now();

        List<EmergencyReleaseHistory> expiredAccessRecords =
                emergencyReleaseHistoryRepository
                        .findByAccessRevokedFalseAndAccessExpiresAtLessThanEqual(
                                now
                        );

        if (expiredAccessRecords.isEmpty()) {

            log.debug(
                    "No expired emergency document access found at {}",
                    now
            );

            return;
        }

        int revokedAccessCount = 0;
        int notificationFailureCount = 0;

        for (EmergencyReleaseHistory accessRecord
                : expiredAccessRecords) {

            if (accessRecord == null
                    || accessRecord.getId() == null) {

                log.warn(
                        "Skipping invalid emergency release history record"
                );

                continue;
            }

            if (Boolean.TRUE.equals(
                    accessRecord.getAccessRevoked()
            )) {

                continue;
            }

            accessRecord.setAccessRevoked(true);

            accessRecord.setCanView(false);

            accessRecord.setCanDownload(false);

            accessRecord.setRevokedAt(now);

            accessRecord.setRevocationReason(
                    AUTOMATIC_EXPIRY_REASON
            );

            emergencyReleaseHistoryRepository.save(
                    accessRecord
            );

            revokedAccessCount++;

            try {

                notificationEventService.accessExpired(
                        accessRecord
                );

            } catch (RuntimeException exception) {

                notificationFailureCount++;

                log.error(
                        "Failed to create access-expiry notification " +
                                "for release history {}",
                        accessRecord.getId(),
                        exception
                );
            }
        }

        log.info(
                "Automatically revoked {} expired document access record(s). " +
                        "Notification failures: {}",
                revokedAccessCount,
                notificationFailureCount
        );
    }
}