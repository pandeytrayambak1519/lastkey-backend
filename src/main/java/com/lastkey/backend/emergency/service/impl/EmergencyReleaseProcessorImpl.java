package com.lastkey.backend.emergency.service.impl;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.document.repository.DocumentRepository;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.entity.EmergencyRequestLog;
import com.lastkey.backend.emergency.enums.EmergencyLogAction;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.exception.EmergencyReleaseException;
import com.lastkey.backend.emergency.exception.EmergencyRequestNotFoundException;
import com.lastkey.backend.emergency.repository.EmergencyReleaseHistoryRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestLogRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestRepository;
import com.lastkey.backend.emergency.service.EmergencyReleaseProcessor;
import com.lastkey.backend.notification.service.NotificationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyReleaseProcessorImpl
        implements EmergencyReleaseProcessor {

    private final EmergencyRequestRepository
            emergencyRequestRepository;

    private final EmergencyRequestLogRepository
            emergencyRequestLogRepository;

    private final EmergencyReleaseHistoryRepository
            emergencyReleaseHistoryRepository;

    private final DocumentRepository documentRepository;

    private final NotificationEventService
            notificationEventService;

    /*
     * Number of days for which nominee access remains valid.
     *
     * Default value: 30 days
     */
    @Value("${lastkey.emergency.access-validity-days:30}")
    private int accessValidityDays;

    /*
     * Every request is processed in an independent transaction.
     *
     * If one request fails, other eligible requests can
     * still be processed successfully.
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void processEmergencyRelease(
            UUID emergencyRequestId
    ) {

        EmergencyRequest emergencyRequest =
                emergencyRequestRepository
                        .findById(emergencyRequestId)
                        .orElseThrow(
                                () ->
                                        new EmergencyRequestNotFoundException(
                                                "Emergency request not found with ID: "
                                                        + emergencyRequestId
                                        )
                        );

        validateRequestForAutomaticRelease(
                emergencyRequest
        );

        List<Document> activeDocuments =
                documentRepository
                        .findByOwnerAndStatus(
                                emergencyRequest.getOwner(),
                                DocumentStatus.ACTIVE
                        );

        if (activeDocuments.isEmpty()) {

            createAuditLog(
                    emergencyRequest,
                    EmergencyLogAction.RELEASE_ATTEMPT_FAILED,
                    emergencyRequest.getStatus(),
                    emergencyRequest.getStatus(),
                    "No active documents were found for automatic release"
            );

            throw new EmergencyReleaseException(
                    "Owner has no active documents available for release"
            );
        }

        LocalDateTime releasedAt =
                LocalDateTime.now();

        LocalDateTime accessExpiresAt =
                releasedAt.plusDays(
                        accessValidityDays
                );

        int releasedDocumentCount = 0;

        for (Document document : activeDocuments) {

            boolean alreadyReleased =
                    emergencyReleaseHistoryRepository
                            .existsByEmergencyRequestAndDocumentAndNominee(
                                    emergencyRequest,
                                    document,
                                    emergencyRequest.getNominee()
                            );

            if (alreadyReleased) {

                log.debug(
                        "Document ID {} is already released for " +
                                "emergency request ID {}",
                        document.getId(),
                        emergencyRequestId
                );

                continue;
            }

            EmergencyReleaseHistory releaseHistory =
                    EmergencyReleaseHistory.builder()
                            .emergencyRequest(
                                    emergencyRequest
                            )
                            .nominee(
                                    emergencyRequest.getNominee()
                            )
                            .document(document)
                            .releasedAt(releasedAt)
                            .accessExpiresAt(
                                    accessExpiresAt
                            )
                            .canView(true)
                            .canDownload(true)
                            .accessRevoked(false)
                            .downloadCount(0)
                            .build();

            emergencyReleaseHistoryRepository.save(
                    releaseHistory
            );

            releasedDocumentCount++;
        }

        if (releasedDocumentCount == 0) {

            createAuditLog(
                    emergencyRequest,
                    EmergencyLogAction.RELEASE_ATTEMPT_FAILED,
                    emergencyRequest.getStatus(),
                    emergencyRequest.getStatus(),
                    "All active documents were already released"
            );

            throw new EmergencyReleaseException(
                    "All active documents have already been released"
            );
        }

        EmergencyStatus previousStatus =
                emergencyRequest.getStatus();

        emergencyRequest.setStatus(
                EmergencyStatus.DOCUMENTS_RELEASED
        );

        emergencyRequest.setReleasedAt(
                releasedAt
        );

        emergencyRequest.setActive(
                false
        );

        emergencyRequestRepository.save(
                emergencyRequest
        );

        createAuditLog(
                emergencyRequest,
                EmergencyLogAction.DOCUMENTS_RELEASED,
                previousStatus,
                EmergencyStatus.DOCUMENTS_RELEASED,
                releasedDocumentCount
                        + " document(s) automatically released to nominee"
        );

        sendDocumentsReleasedNotification(
                emergencyRequest,
                releasedDocumentCount
        );

        log.info(
                "{} document(s) automatically released for " +
                        "emergency request ID: {}. Access expires at: {}",
                releasedDocumentCount,
                emergencyRequestId,
                accessExpiresAt
        );
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE EMERGENCY REQUEST
     * ---------------------------------------------------------
     */

    private void validateRequestForAutomaticRelease(
            EmergencyRequest emergencyRequest
    ) {

        if (emergencyRequest.getStatus()
                != EmergencyStatus.APPROVED) {

            throw new EmergencyReleaseException(
                    "Only APPROVED emergency requests can be processed"
            );
        }

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new EmergencyReleaseException(
                    "Inactive emergency request cannot be processed"
            );
        }

        if (emergencyRequest.getScheduledReleaseAt()
                == null) {

            throw new EmergencyReleaseException(
                    "Scheduled release date is missing"
            );
        }

        if (emergencyRequest
                .getScheduledReleaseAt()
                .isAfter(LocalDateTime.now())) {

            throw new EmergencyReleaseException(
                    "Scheduled release time has not arrived yet"
            );
        }

        if (emergencyRequest.getOwner() == null) {

            throw new EmergencyReleaseException(
                    "Emergency request owner is missing"
            );
        }

        if (emergencyRequest.getNominee() == null) {

            throw new EmergencyReleaseException(
                    "Emergency request nominee is missing"
            );
        }

        if (Boolean.FALSE.equals(
                emergencyRequest
                        .getNominee()
                        .getActive()
        )) {

            throw new EmergencyReleaseException(
                    "Documents cannot be released to an inactive nominee"
            );
        }

        if (accessValidityDays <= 0) {

            throw new EmergencyReleaseException(
                    "Emergency access-validity days must be greater than zero"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * SEND NOTIFICATION
     * ---------------------------------------------------------
     */

    private void sendDocumentsReleasedNotification(
            EmergencyRequest emergencyRequest,
            int releasedDocumentCount
    ) {

        try {

            notificationEventService.documentsReleased(
                    emergencyRequest,
                    releasedDocumentCount
            );

        } catch (Exception exception) {

            /*
             * Notification failure should not cancel the
             * document release transaction.
             */
            log.error(
                    "Documents were released successfully, but " +
                            "notification failed for emergency request ID: {}",
                    emergencyRequest.getId(),
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
            EmergencyRequest emergencyRequest,
            EmergencyLogAction action,
            EmergencyStatus previousStatus,
            EmergencyStatus newStatus,
            String message
    ) {

        EmergencyRequestLog logEntry =
                EmergencyRequestLog.builder()
                        .emergencyRequest(
                                emergencyRequest
                        )
                        .action(action)
                        .previousStatus(
                                previousStatus
                        )
                        .newStatus(newStatus)
                        .performedBy("SYSTEM")
                        .performedByType("SYSTEM")
                        .message(message)
                        .build();

        emergencyRequestLogRepository.save(
                logEntry
        );
    }
}