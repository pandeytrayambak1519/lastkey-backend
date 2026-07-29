package com.lastkey.backend.emergency.scheduler;

import com.lastkey.backend.emergency.service.AutomaticEmergencyReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmergencyReleaseScheduler {

    private final AutomaticEmergencyReleaseService
            automaticEmergencyReleaseService;

    /*
     * Runs every five minutes.
     *
     * fixedDelayString is configurable through application.properties.
     * The next execution starts after the previous execution finishes.
     */
    @Scheduled(
            fixedDelayString =
                    "${lastkey.emergency.release-scheduler-delay:300000}",
            initialDelayString =
                    "${lastkey.emergency.release-scheduler-initial-delay:60000}"
    )
    public void releaseEligibleDocuments() {

        log.debug(
                "Starting automatic emergency document-release scheduler"
        );

        try {

            automaticEmergencyReleaseService
                    .processScheduledReleases();

        } catch (Exception exception) {

            log.error(
                    "Unexpected error occurred while processing " +
                            "automatic emergency document releases",
                    exception
            );
        }
    }
}