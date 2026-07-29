package com.lastkey.backend.notification.scheduler;

import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.repository.EmergencyRequestRepository;
import com.lastkey.backend.emergency.service.EmergencyAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyScheduler {

    private final EmergencyRequestRepository
            emergencyRequestRepository;

    private final EmergencyAutomationService
            emergencyAutomationService;

    /*
     * Runs every 15 minutes by default.
     *
     * Override with:
     * lastkey.scheduler.emergency-release.cron
     */
    @Scheduled(
            cron = "${lastkey.scheduler.emergency-release.cron:0 */15 * * * *}"
    )
    public void releaseEligibleEmergencyDocuments() {

        LocalDateTime now =
                LocalDateTime.now();

        List<EmergencyRequest> eligibleRequests =
                emergencyRequestRepository
                        .findByStatusAndActiveTrueAndScheduledReleaseAtLessThanEqual(
                                EmergencyStatus.APPROVED,
                                now
                        );

        if (eligibleRequests.isEmpty()) {
            log.debug(
                    "No emergency requests are ready for automatic release at {}",
                    now
            );
            return;
        }

        int successfulReleases = 0;
        int failedReleases = 0;

        for (EmergencyRequest request
                : eligibleRequests) {

            try {

                boolean released =
                        emergencyAutomationService
                                .releaseDocumentsAutomatically(
                                        request.getId()
                                );

                if (released) {
                    successfulReleases++;
                } else {
                    failedReleases++;
                }

            } catch (Exception exception) {

                failedReleases++;

                log.error(
                        "Automatic document release failed for emergency request {}",
                        request.getId(),
                        exception
                );
            }
        }

        log.info(
                "Emergency release scheduler completed. " +
                        "Eligible: {}, successful: {}, skipped/failed: {}",
                eligibleRequests.size(),
                successfulReleases,
                failedReleases
        );
    }
}