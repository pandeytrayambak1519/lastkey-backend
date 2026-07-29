package com.lastkey.backend.emergency.service.impl;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.document.repository.DocumentRepository;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.entity.EmergencyRequestLog;
import com.lastkey.backend.emergency.enums.EmergencyLogAction;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.repository.EmergencyReleaseHistoryRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestLogRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestRepository;
import com.lastkey.backend.emergency.service.EmergencyAutomationService;
import com.lastkey.backend.notification.service.NotificationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyAutomationServiceImpl
        implements EmergencyAutomationService {

    private static final int ACCESS_VALIDITY_DAYS = 30;

    private final EmergencyRequestRepository
            emergencyRequestRepository;

    private final EmergencyReleaseHistoryRepository
            emergencyReleaseHistoryRepository;

    private final EmergencyRequestLogRepository
            emergencyRequestLogRepository;

    private final DocumentRepository documentRepository;

    private final NotificationEventService
            notificationEventService;

    @Override
    @Transactional
    public boolean releaseDocumentsAutomatically(
            UUID emergencyRequestId
    ) {

        EmergencyRequest emergencyRequest =
                emergencyRequestRepository
                        .findById(emergencyRequestId)
                        .orElse(null);

        if (emergencyRequest == null) {
            log.warn(
                    "Automatic release skipped. Emergency request {} was not found",
                    emergencyRequestId
            );
            return false;
        }

        if (!isEligibleForAutomaticRelease(
                emergencyRequest
        )) {
            log.info(
                    "Automatic release skipped for request {} because it is no longer eligible",
                    emergencyRequestId
            );
            return false;
        }

        List<Document> documents =
                documentRepository.findByOwnerAndStatus(
                        emergencyRequest.getOwner(),
                        DocumentStatus.ACTIVE
                );

        if (documents.isEmpty()) {

            createAuditLog(
                    emergencyRequest,
                    EmergencyLogAction.RELEASE_ATTEMPT_FAILED,
                    emergencyRequest.getStatus(),
                    emergencyRequest.getStatus(),
                    "SYSTEM",
                    "SYSTEM",
                    "Automatic document release failed because " +
                            "the owner has no active documents"
            );

            log.warn(
                    "Automatic release failed for request {} because no active documents exist",
                    emergencyRequestId
            );

            return false;
        }

        LocalDateTime releaseTime =
                LocalDateTime.now();

        LocalDateTime accessExpiryTime =
                releaseTime.plusDays(
                        ACCESS_VALIDITY_DAYS
                );

        int releasedDocumentCount = 0;

        for (Document document : documents) {

            boolean alreadyReleased =
                    emergencyReleaseHistoryRepository
                            .existsByEmergencyRequestAndDocumentAndNominee(
                                    emergencyRequest,
                                    document,
                                    emergencyRequest.getNominee()
                            );

            if (alreadyReleased) {
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
                            .canView(true)
                            .canDownload(true)
                            .releasedAt(releaseTime)
                            .accessExpiresAt(
                                    accessExpiryTime
                            )
                            .accessRevoked(false)
                            .downloadCount(0)
                            .build();

            emergencyReleaseHistoryRepository.save(
                    releaseHistory
            );

            releasedDocumentCount++;
        }

        if (releasedDocumentCount == 0) {

            log.info(
                    "Automatic release skipped for request {} because all documents were already released",
                    emergencyRequestId
            );

            return false;
        }

        EmergencyStatus previousStatus =
                emergencyRequest.getStatus();

        emergencyRequest.setStatus(
                EmergencyStatus.DOCUMENTS_RELEASED
        );

        emergencyRequest.setReleasedAt(
                releaseTime
        );

        emergencyRequest.setActive(false);

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.DOCUMENTS_RELEASED,
                previousStatus,
                EmergencyStatus.DOCUMENTS_RELEASED,
                "SYSTEM",
                "SYSTEM",
                releasedDocumentCount
                        + " document(s) released automatically to nominee "
                        + savedRequest.getNominee().getEmail()
        );

        notificationEventService.documentsReleased(
                savedRequest,
                releasedDocumentCount
        );

        log.info(
                "Automatically released {} document(s) for emergency request {}",
                releasedDocumentCount,
                emergencyRequestId
        );

        return true;
    }

    private boolean isEligibleForAutomaticRelease(
            EmergencyRequest emergencyRequest
    ) {

        if (emergencyRequest.getStatus()
                != EmergencyStatus.APPROVED) {
            return false;
        }

        if (!Boolean.TRUE.equals(
                emergencyRequest.getActive()
        )) {
            return false;
        }

        LocalDateTime scheduledReleaseAt =
                emergencyRequest
                        .getScheduledReleaseAt();

        return scheduledReleaseAt != null
                && !scheduledReleaseAt.isAfter(
                        LocalDateTime.now()
                );
    }

    private void createAuditLog(
            EmergencyRequest emergencyRequest,
            EmergencyLogAction action,
            EmergencyStatus previousStatus,
            EmergencyStatus newStatus,
            String performedBy,
            String performedByType,
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
                        .performedBy(performedBy)
                        .performedByType(
                                performedByType
                        )
                        .message(message)
                        .build();

        emergencyRequestLogRepository.save(
                logEntry
        );
    }
}