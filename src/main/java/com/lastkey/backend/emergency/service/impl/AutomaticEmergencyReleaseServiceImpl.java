package com.lastkey.backend.emergency.service.impl;

import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.repository.EmergencyRequestRepository;
import com.lastkey.backend.emergency.service.AutomaticEmergencyReleaseService;
import com.lastkey.backend.emergency.service.EmergencyReleaseProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomaticEmergencyReleaseServiceImpl
        implements AutomaticEmergencyReleaseService {

    private final EmergencyRequestRepository
            emergencyRequestRepository;

    private final EmergencyReleaseProcessor
            emergencyReleaseProcessor;

    /*
     * ---------------------------------------------------------
     * FIND AND PROCESS ELIGIBLE REQUESTS
     * ---------------------------------------------------------
     */

    @Override
    @Transactional(readOnly = true)
    public void processScheduledReleases() {

        LocalDateTime currentTime =
                LocalDateTime.now();

        List<EmergencyRequest> eligibleRequests =
                emergencyRequestRepository
                        .findByStatusAndActiveTrueAndScheduledReleaseAtLessThanEqual(
                                EmergencyStatus.APPROVED,
                                currentTime
                        );

        if (eligibleRequests.isEmpty()) {

            log.debug(
                    "No emergency requests are eligible " +
                            "for automatic document release"
            );

            return;
        }

        log.info(
                "Found {} emergency request(s) eligible " +
                        "for automatic release",
                eligibleRequests.size()
        );

        for (EmergencyRequest emergencyRequest
                : eligibleRequests) {

            try {

                emergencyReleaseProcessor
                        .processEmergencyRelease(
                                emergencyRequest.getId()
                        );

            } catch (Exception exception) {

                log.error(
                        "Automatic document release failed for " +
                                "emergency request ID: {}",
                        emergencyRequest.getId(),
                        exception
                );
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * MANUALLY PROCESS ONE REQUEST THROUGH PROCESSOR
     * ---------------------------------------------------------
     */

    @Override
    public void processSingleRequest(
            UUID emergencyRequestId
    ) {

        emergencyReleaseProcessor
                .processEmergencyRelease(
                        emergencyRequestId
                );
    }
}