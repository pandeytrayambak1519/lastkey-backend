package com.lastkey.backend.emergency.service.impl;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.document.repository.DocumentRepository;
import com.lastkey.backend.emergency.dto.request.CreateEmergencyRequest;
import com.lastkey.backend.emergency.dto.request.EmergencyActionRequest;
import com.lastkey.backend.emergency.dto.request.UpdateEmergencyRequest;
import com.lastkey.backend.emergency.dto.response.EmergencyHistoryResponse;
import com.lastkey.backend.emergency.dto.response.EmergencyReleaseResponse;
import com.lastkey.backend.emergency.dto.response.EmergencyResponse;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.entity.EmergencyRequestLog;
import com.lastkey.backend.emergency.enums.EmergencyLogAction;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.enums.EmergencyTriggerType;
import com.lastkey.backend.emergency.exception.ActiveEmergencyRequestAlreadyExistsException;
import com.lastkey.backend.emergency.exception.EmergencyAccessDeniedException;
import com.lastkey.backend.emergency.exception.EmergencyReleaseException;
import com.lastkey.backend.emergency.exception.EmergencyRequestNotFoundException;
import com.lastkey.backend.emergency.exception.InvalidEmergencyStatusException;
import com.lastkey.backend.emergency.mapper.EmergencyHistoryMapper;
import com.lastkey.backend.emergency.mapper.EmergencyMapper;
import com.lastkey.backend.emergency.mapper.EmergencyReleaseMapper;
import com.lastkey.backend.emergency.repository.EmergencyReleaseHistoryRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestLogRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestRepository;
import com.lastkey.backend.emergency.service.EmergencyService;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.exception.NomineeNotFoundException;
import com.lastkey.backend.nominee.repository.NomineeRepository;
import com.lastkey.backend.notification.service.NotificationEventService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmergencyServiceImpl implements EmergencyService {

    private final EmergencyRequestRepository emergencyRequestRepository;

    private final EmergencyRequestLogRepository emergencyRequestLogRepository;

    private final EmergencyReleaseHistoryRepository
            emergencyReleaseHistoryRepository;

    private final NomineeRepository nomineeRepository;

    private final UserRepository userRepository;

    private final DocumentRepository documentRepository;

    private final EmergencyMapper emergencyMapper;

    private final EmergencyHistoryMapper emergencyHistoryMapper;

    private final EmergencyReleaseMapper emergencyReleaseMapper;

    private final NotificationEventService notificationEventService;

    private static final List<EmergencyStatus> ACTIVE_STATUSES =
            List.of(
                    EmergencyStatus.PENDING,
                    EmergencyStatus.OWNER_NOTIFIED,
                    EmergencyStatus.WAITING_PERIOD,
                    EmergencyStatus.APPROVED
            );

    /*
     * ---------------------------------------------------------
     * CREATE EMERGENCY REQUEST
     * ---------------------------------------------------------
     */

    @Override
    public EmergencyResponse createEmergencyRequest(
            CreateEmergencyRequest request,
            String currentUserEmail
    ) {

        User owner = getCurrentUser(
                currentUserEmail
        );

        Nominee nominee = getOwnedNominee(
                request.getNomineeId(),
                owner
        );

        validateNomineeIsActive(
                nominee
        );

        validateNoActiveEmergencyRequest(
                owner,
                nominee
        );

        EmergencyTriggerType triggerType =
                request.getTriggerType() != null
                        ? request.getTriggerType()
                        : EmergencyTriggerType.OWNER_CREATED;

        if (triggerType
                != EmergencyTriggerType.OWNER_CREATED) {

            throw new EmergencyAccessDeniedException(
                    "An owner can only create an emergency request " +
                            "with trigger type OWNER_CREATED"
            );
        }

        int waitingPeriodDays =
                request.getWaitingPeriodDays() != null
                        ? request.getWaitingPeriodDays()
                        : 7;

        LocalDateTime now =
                LocalDateTime.now();

        EmergencyRequest emergencyRequest =
                EmergencyRequest.builder()
                        .owner(owner)
                        .nominee(nominee)
                        .status(
                                EmergencyStatus.WAITING_PERIOD
                        )
                        .triggerType(triggerType)
                        .reason(
                                request.getReason().trim()
                        )
                        .evidenceUrl(
                                normalizeNullable(
                                        request.getEvidenceUrl()
                                )
                        )
                        .waitingPeriodDays(
                                waitingPeriodDays
                        )
                        .ownerNotifiedAt(now)
                        .scheduledReleaseAt(
                                now.plusDays(
                                        waitingPeriodDays
                                )
                        )
                        .active(true)
                        .build();

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.REQUEST_CREATED,
                null,
                EmergencyStatus.PENDING,
                owner.getEmail(),
                "OWNER",
                "Emergency request created"
        );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.OWNER_NOTIFIED,
                EmergencyStatus.PENDING,
                EmergencyStatus.OWNER_NOTIFIED,
                "SYSTEM",
                "SYSTEM",
                "Owner notification process initiated"
        );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.WAITING_PERIOD_STARTED,
                EmergencyStatus.OWNER_NOTIFIED,
                EmergencyStatus.WAITING_PERIOD,
                "SYSTEM",
                "SYSTEM",
                "Waiting period started for "
                        + waitingPeriodDays
                        + " day(s)"
        );

        notificationEventService.emergencyCreated(
                savedRequest
        );

        return emergencyMapper.toResponse(
                savedRequest
        );
    }

    /*
     * ---------------------------------------------------------
     * GET EMERGENCY REQUEST BY ID
     * ---------------------------------------------------------
     */

    @Override
    @Transactional(readOnly = true)
    public EmergencyResponse getEmergencyRequestById(
            UUID emergencyRequestId,
            String currentUserEmail
    ) {

        User currentUser =
                getCurrentUser(
                        currentUserEmail
                );

        EmergencyRequest emergencyRequest =
                getEmergencyRequestEntity(
                        emergencyRequestId
                );

        validateOwnerAccess(
                emergencyRequest,
                currentUser
        );

        return emergencyMapper.toResponse(
                emergencyRequest
        );
    }

    /*
     * ---------------------------------------------------------
     * GET ALL REQUESTS OF CURRENT OWNER
     * ---------------------------------------------------------
     */

    @Override
    @Transactional(readOnly = true)
    public Page<EmergencyResponse> getOwnerEmergencyRequests(
            String currentUserEmail,
            Pageable pageable
    ) {

        User owner = getCurrentUser(
                currentUserEmail
        );

        return emergencyRequestRepository
                .findByOwner(
                        owner,
                        pageable
                )
                .map(
                        emergencyMapper::toResponse
                );
    }

    /*
     * ---------------------------------------------------------
     * UPDATE EMERGENCY REQUEST
     * ---------------------------------------------------------
     */

    @Override
    public EmergencyResponse updateEmergencyRequest(
            UUID emergencyRequestId,
            UpdateEmergencyRequest request,
            String currentUserEmail
    ) {

        User owner = getCurrentUser(
                currentUserEmail
        );

        EmergencyRequest emergencyRequest =
                getOwnedEmergencyRequest(
                        emergencyRequestId,
                        owner
                );

        validateRequestCanBeUpdated(
                emergencyRequest
        );

        boolean changed = false;
        boolean evidenceChanged = false;

        if (request.getReason() != null) {

            String reason =
                    request.getReason().trim();

            if (reason.isBlank()) {
                throw new IllegalArgumentException(
                        "Emergency reason cannot be empty"
                );
            }

            emergencyRequest.setReason(
                    reason
            );

            changed = true;
        }

        if (request.getEvidenceUrl() != null) {

            emergencyRequest.setEvidenceUrl(
                    normalizeNullable(
                            request.getEvidenceUrl()
                    )
            );

            changed = true;
            evidenceChanged = true;
        }

        if (request.getWaitingPeriodDays() != null) {

            int waitingPeriodDays =
                    request.getWaitingPeriodDays();

            emergencyRequest.setWaitingPeriodDays(
                    waitingPeriodDays
            );

            LocalDateTime waitingPeriodStart =
                    emergencyRequest
                            .getOwnerNotifiedAt() != null
                            ? emergencyRequest
                            .getOwnerNotifiedAt()
                            : LocalDateTime.now();

            emergencyRequest.setScheduledReleaseAt(
                    waitingPeriodStart.plusDays(
                            waitingPeriodDays
                    )
            );

            changed = true;
        }

        if (!changed) {
            throw new IllegalArgumentException(
                    "No fields were provided for update"
            );
        }

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        if (evidenceChanged) {

            createAuditLog(
                    savedRequest,
                    EmergencyLogAction.EVIDENCE_UPDATED,
                    savedRequest.getStatus(),
                    savedRequest.getStatus(),
                    owner.getEmail(),
                    "OWNER",
                    "Emergency evidence was updated"
            );
        }

        createAuditLog(
                savedRequest,
                EmergencyLogAction.STATUS_UPDATED,
                savedRequest.getStatus(),
                savedRequest.getStatus(),
                owner.getEmail(),
                "OWNER",
                "Emergency request details updated"
        );

        notificationEventService.emergencyUpdated(
                savedRequest
        );

        return emergencyMapper.toResponse(
                savedRequest
        );
    }

    /*
     * ---------------------------------------------------------
     * CANCEL EMERGENCY REQUEST
     * ---------------------------------------------------------
     */

    @Override
    public EmergencyResponse cancelEmergencyRequest(
            UUID emergencyRequestId,
            EmergencyActionRequest request,
            String currentUserEmail
    ) {

        User owner = getCurrentUser(
                currentUserEmail
        );

        EmergencyRequest emergencyRequest =
                getOwnedEmergencyRequest(
                        emergencyRequestId,
                        owner
                );

        validateRequestCanBeCancelled(
                emergencyRequest
        );

        EmergencyStatus previousStatus =
                emergencyRequest.getStatus();

        String cancellationMessage =
                request != null
                        ? normalizeNullable(
                                request.getMessage()
                        )
                        : null;

        emergencyRequest.setStatus(
                EmergencyStatus.CANCELLED
        );

        emergencyRequest.setCancelledAt(
                LocalDateTime.now()
        );

        emergencyRequest.setOwnerResponseMessage(
                cancellationMessage
        );

        emergencyRequest.setActive(
                false
        );

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.OWNER_CANCELLED,
                previousStatus,
                EmergencyStatus.CANCELLED,
                owner.getEmail(),
                "OWNER",
                cancellationMessage != null
                        ? cancellationMessage
                        : "Emergency request cancelled by owner"
        );

        notificationEventService.emergencyCancelled(
                savedRequest
        );

        return emergencyMapper.toResponse(
                savedRequest
        );
    }

    /*
     * ---------------------------------------------------------
     * APPROVE EMERGENCY REQUEST
     * ---------------------------------------------------------
     */

    @Override
    public EmergencyResponse approveEmergencyRequest(
            UUID emergencyRequestId,
            EmergencyActionRequest request,
            String performedByEmail
    ) {

        User performedBy = getCurrentUser(
                performedByEmail
        );

        EmergencyRequest emergencyRequest =
                getEmergencyRequestEntity(
                        emergencyRequestId
                );

        validateRequestCanBeApproved(
                emergencyRequest
        );

        EmergencyStatus previousStatus =
                emergencyRequest.getStatus();

        String approvalMessage =
                request != null
                        ? normalizeNullable(
                                request.getMessage()
                        )
                        : null;

        emergencyRequest.setStatus(
                EmergencyStatus.APPROVED
        );

        emergencyRequest.setApprovedAt(
                LocalDateTime.now()
        );

        emergencyRequest.setAdminReviewMessage(
                approvalMessage
        );

        emergencyRequest.setActive(
                true
        );

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.ADMIN_APPROVED,
                previousStatus,
                EmergencyStatus.APPROVED,
                performedBy.getEmail(),
                "ADMIN",
                approvalMessage != null
                        ? approvalMessage
                        : "Emergency request approved by admin"
        );

        notificationEventService.emergencyApproved(
                savedRequest
        );

        return emergencyMapper.toResponse(
                savedRequest
        );
    }

    /*
     * ---------------------------------------------------------
     * REJECT EMERGENCY REQUEST
     * ---------------------------------------------------------
     */

    @Override
    public EmergencyResponse rejectEmergencyRequest(
            UUID emergencyRequestId,
            EmergencyActionRequest request,
            String performedByEmail
    ) {

        User performedBy = getCurrentUser(
                performedByEmail
        );

        EmergencyRequest emergencyRequest =
                getEmergencyRequestEntity(
                        emergencyRequestId
                );

        validateRequestCanBeRejected(
                emergencyRequest
        );

        String rejectionMessage =
                request != null
                        ? normalizeNullable(
                                request.getMessage()
                        )
                        : null;

        if (rejectionMessage == null) {
            throw new IllegalArgumentException(
                    "Rejection reason is required"
            );
        }

        EmergencyStatus previousStatus =
                emergencyRequest.getStatus();

        emergencyRequest.setStatus(
                EmergencyStatus.REJECTED
        );

        emergencyRequest.setRejectedAt(
                LocalDateTime.now()
        );

        emergencyRequest.setAdminReviewMessage(
                rejectionMessage
        );

        emergencyRequest.setActive(
                false
        );

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.ADMIN_REJECTED,
                previousStatus,
                EmergencyStatus.REJECTED,
                performedBy.getEmail(),
                "ADMIN",
                rejectionMessage
        );

        notificationEventService.emergencyRejected(
                savedRequest
        );

        return emergencyMapper.toResponse(
                savedRequest
        );
    }

    /*
     * ---------------------------------------------------------
     * GET EMERGENCY REQUEST HISTORY
     * ---------------------------------------------------------
     */

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyHistoryResponse>
    getEmergencyRequestHistory(
            UUID emergencyRequestId,
            String currentUserEmail
    ) {

        User currentUser = getCurrentUser(
                currentUserEmail
        );

        EmergencyRequest emergencyRequest =
                getEmergencyRequestEntity(
                        emergencyRequestId
                );

        validateOwnerAccess(
                emergencyRequest,
                currentUser
        );

        return emergencyRequestLogRepository
                .findByEmergencyRequestOrderByCreatedAtAsc(
                        emergencyRequest
                )
                .stream()
                .map(
                        emergencyHistoryMapper::toResponse
                )
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * RELEASE OWNER DOCUMENTS TO NOMINEE
     * ---------------------------------------------------------
     */

    @Override
    public List<EmergencyReleaseResponse> releaseDocuments(
            UUID emergencyRequestId,
            String performedByEmail
    ) {

        User performedBy = getCurrentUser(
                performedByEmail
        );

        EmergencyRequest emergencyRequest =
                getEmergencyRequestEntity(
                        emergencyRequestId
                );

        validateRequestCanReleaseDocuments(
                emergencyRequest
        );

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
                    performedBy.getEmail(),
                    "ADMIN",
                    "Document release failed because the owner " +
                            "has no active documents"
            );

            throw new EmergencyReleaseException(
                    "The owner has no active documents available for release"
            );
        }

        LocalDateTime releaseTime =
                LocalDateTime.now();

        LocalDateTime accessExpiryTime =
                releaseTime.plusDays(30);

        List<EmergencyReleaseResponse> responses =
                new ArrayList<>();

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
                            .document(
                                    document
                            )
                            .canView(true)
                            .canDownload(true)
                            .releasedAt(
                                    releaseTime
                            )
                            .accessExpiresAt(
                                    accessExpiryTime
                            )
                            .accessRevoked(false)
                            .downloadCount(0)
                            .build();

            EmergencyReleaseHistory savedRelease =
                    emergencyReleaseHistoryRepository.save(
                            releaseHistory
                    );

            responses.add(
                    emergencyReleaseMapper.toResponse(
                            savedRelease
                    )
            );
        }

        if (responses.isEmpty()) {
            throw new EmergencyReleaseException(
                    "All active documents have already been released " +
                            "for this emergency request"
            );
        }

        EmergencyStatus previousStatus =
                emergencyRequest.getStatus();

        emergencyRequest.setStatus(
                EmergencyStatus.DOCUMENTS_RELEASED
        );

        emergencyRequest.setReleasedAt(
                releaseTime
        );

        emergencyRequest.setActive(
                false
        );

        EmergencyRequest savedRequest =
                emergencyRequestRepository.save(
                        emergencyRequest
                );

        createAuditLog(
                savedRequest,
                EmergencyLogAction.DOCUMENTS_RELEASED,
                previousStatus,
                EmergencyStatus.DOCUMENTS_RELEASED,
                performedBy.getEmail(),
                "ADMIN",
                responses.size()
                        + " document(s) released to nominee "
                        + emergencyRequest.getNominee().getEmail()
        );

        notificationEventService.documentsReleased(
                savedRequest,
                responses.size()
        );

        return responses;
    }

    /*
     * ---------------------------------------------------------
     * GET RELEASED DOCUMENTS
     * ---------------------------------------------------------
     */

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyReleaseResponse> getReleasedDocuments(
            UUID emergencyRequestId,
            String currentUserEmail
    ) {

        User currentUser = getCurrentUser(
                currentUserEmail
        );

        EmergencyRequest emergencyRequest =
                getEmergencyRequestEntity(
                        emergencyRequestId
                );

        validateOwnerAccess(
                emergencyRequest,
                currentUser
        );

        return emergencyReleaseHistoryRepository
                .findByEmergencyRequestOrderByReleasedAtDesc(
                        emergencyRequest
                )
                .stream()
                .map(
                        emergencyReleaseMapper::toResponse
                )
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * REVOKE RELEASED DOCUMENT ACCESS
     * ---------------------------------------------------------
     */

    @Override
    public void revokeReleasedDocument(
            UUID releaseHistoryId,
            String reason,
            String performedByEmail
    ) {

        User performedBy = getCurrentUser(
                performedByEmail
        );

        String normalizedReason =
                normalizeNullable(reason);

        if (normalizedReason == null) {
            throw new IllegalArgumentException(
                    "Access revocation reason is required"
            );
        }

        EmergencyReleaseHistory releaseHistory =
                emergencyReleaseHistoryRepository
                        .findById(releaseHistoryId)
                        .orElseThrow(
                                () ->
                                        new EmergencyReleaseException(
                                                "Document release history not found with ID: "
                                                        + releaseHistoryId
                                        )
                        );

        if (Boolean.TRUE.equals(
                releaseHistory.getAccessRevoked()
        )) {
            throw new EmergencyReleaseException(
                    "Access to this document has already been revoked"
            );
        }

        releaseHistory.setAccessRevoked(
                true
        );

        releaseHistory.setRevokedAt(
                LocalDateTime.now()
        );

        releaseHistory.setRevocationReason(
                normalizedReason
        );

        releaseHistory.setCanView(
                false
        );

        releaseHistory.setCanDownload(
                false
        );

        emergencyReleaseHistoryRepository.save(
                releaseHistory
        );

        EmergencyRequest emergencyRequest =
                releaseHistory.getEmergencyRequest();

        createAuditLog(
                emergencyRequest,
                EmergencyLogAction.ACCESS_REVOKED,
                emergencyRequest.getStatus(),
                emergencyRequest.getStatus(),
                performedBy.getEmail(),
                "ADMIN",
                normalizedReason
        );
    }

    /*
     * ---------------------------------------------------------
     * GET CURRENT AUTHENTICATED USER
     * ---------------------------------------------------------
     */

    private User getCurrentUser(
            String currentUserEmail
    ) {

        if (currentUserEmail == null
                || currentUserEmail.isBlank()) {

            throw new UsernameNotFoundException(
                    "Authenticated user email is missing"
            );
        }

        String normalizedEmail =
                currentUserEmail
                        .trim()
                        .toLowerCase();

        return userRepository
                .findByEmail(
                        normalizedEmail
                )
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "User not found with email: "
                                                + normalizedEmail
                                )
                );
    }

    /*
     * ---------------------------------------------------------
     * GET EMERGENCY REQUEST ENTITY
     * ---------------------------------------------------------
     */

    private EmergencyRequest getEmergencyRequestEntity(
            UUID emergencyRequestId
    ) {

        return emergencyRequestRepository
                .findById(
                        emergencyRequestId
                )
                .orElseThrow(
                        () ->
                                new EmergencyRequestNotFoundException(
                                        "Emergency request not found with ID: "
                                                + emergencyRequestId
                                )
                );
    }

    /*
     * ---------------------------------------------------------
     * FIND OWNER'S EMERGENCY REQUEST
     * ---------------------------------------------------------
     */

    private EmergencyRequest getOwnedEmergencyRequest(
            UUID emergencyRequestId,
            User owner
    ) {

        EmergencyRequest emergencyRequest =
                getEmergencyRequestEntity(
                        emergencyRequestId
                );

        validateOwnerAccess(
                emergencyRequest,
                owner
        );

        return emergencyRequest;
    }

    /*
     * ---------------------------------------------------------
     * FIND NOMINEE OWNED BY USER
     * ---------------------------------------------------------
     */

    private Nominee getOwnedNominee(
            UUID nomineeId,
            User owner
    ) {

        Nominee nominee =
                nomineeRepository
                        .findById(
                                nomineeId
                        )
                        .orElseThrow(
                                () ->
                                        new NomineeNotFoundException(
                                                "Nominee not found with ID: "
                                                        + nomineeId
                                        )
                        );

        if (nominee.getOwner() == null
                || nominee.getOwner().getId() == null
                || !nominee.getOwner()
                .getId()
                .equals(owner.getId())) {

            throw new EmergencyAccessDeniedException(
                    "You cannot access another user's nominee"
            );
        }

        return nominee;
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE NOMINEE STATUS
     * ---------------------------------------------------------
     */

    private void validateNomineeIsActive(
            Nominee nominee
    ) {

        if (Boolean.FALSE.equals(
                nominee.getActive()
        )) {

            throw new EmergencyAccessDeniedException(
                    "Emergency request cannot be created " +
                            "for an inactive nominee"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * PREVENT DUPLICATE ACTIVE REQUEST
     * ---------------------------------------------------------
     */

    private void validateNoActiveEmergencyRequest(
            User owner,
            Nominee nominee
    ) {

        boolean activeRequestExists =
                emergencyRequestRepository
                        .existsByOwnerAndNomineeAndStatusIn(
                                owner,
                                nominee,
                                ACTIVE_STATUSES
                        );

        if (activeRequestExists) {

            throw new ActiveEmergencyRequestAlreadyExistsException(
                    "An active emergency request already exists " +
                            "for this nominee"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE OWNER ACCESS
     * ---------------------------------------------------------
     */

    private void validateOwnerAccess(
            EmergencyRequest emergencyRequest,
            User currentUser
    ) {

        if (emergencyRequest.getOwner() == null
                || emergencyRequest.getOwner().getId() == null
                || !emergencyRequest.getOwner()
                .getId()
                .equals(currentUser.getId())) {

            throw new EmergencyAccessDeniedException(
                    "You do not have permission to access " +
                            "this emergency request"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE REQUEST CAN BE UPDATED
     * ---------------------------------------------------------
     */

    private void validateRequestCanBeUpdated(
            EmergencyRequest emergencyRequest
    ) {

        EmergencyStatus status =
                emergencyRequest.getStatus();

        if (status != EmergencyStatus.PENDING
                && status != EmergencyStatus.OWNER_NOTIFIED
                && status != EmergencyStatus.WAITING_PERIOD) {

            throw new InvalidEmergencyStatusException(
                    "Emergency request cannot be updated " +
                            "when its status is " + status
            );
        }

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new InvalidEmergencyStatusException(
                    "Inactive emergency request cannot be updated"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE REQUEST CAN BE CANCELLED
     * ---------------------------------------------------------
     */

    private void validateRequestCanBeCancelled(
            EmergencyRequest emergencyRequest
    ) {

        EmergencyStatus status =
                emergencyRequest.getStatus();

        if (status != EmergencyStatus.PENDING
                && status != EmergencyStatus.OWNER_NOTIFIED
                && status != EmergencyStatus.WAITING_PERIOD
                && status != EmergencyStatus.APPROVED) {

            throw new InvalidEmergencyStatusException(
                    "Emergency request cannot be cancelled " +
                            "when its status is " + status
            );
        }

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new InvalidEmergencyStatusException(
                    "Inactive emergency request cannot be cancelled"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE REQUEST CAN BE APPROVED
     * ---------------------------------------------------------
     */

    private void validateRequestCanBeApproved(
            EmergencyRequest emergencyRequest
    ) {

        EmergencyStatus status =
                emergencyRequest.getStatus();

        if (status != EmergencyStatus.PENDING
                && status != EmergencyStatus.OWNER_NOTIFIED
                && status != EmergencyStatus.WAITING_PERIOD) {

            throw new InvalidEmergencyStatusException(
                    "Emergency request cannot be approved " +
                            "when its status is " + status
            );
        }

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new InvalidEmergencyStatusException(
                    "Inactive emergency request cannot be approved"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE REQUEST CAN BE REJECTED
     * ---------------------------------------------------------
     */

    private void validateRequestCanBeRejected(
            EmergencyRequest emergencyRequest
    ) {

        EmergencyStatus status =
                emergencyRequest.getStatus();

        if (status != EmergencyStatus.PENDING
                && status != EmergencyStatus.OWNER_NOTIFIED
                && status != EmergencyStatus.WAITING_PERIOD) {

            throw new InvalidEmergencyStatusException(
                    "Emergency request cannot be rejected " +
                            "when its status is " + status
            );
        }

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new InvalidEmergencyStatusException(
                    "Inactive emergency request cannot be rejected"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * VALIDATE DOCUMENT RELEASE
     * ---------------------------------------------------------
     */

    private void validateRequestCanReleaseDocuments(
            EmergencyRequest emergencyRequest
    ) {

        if (emergencyRequest.getStatus()
                != EmergencyStatus.APPROVED) {

            throw new EmergencyReleaseException(
                    "Documents can only be released when " +
                            "the emergency request is APPROVED"
            );
        }

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new EmergencyReleaseException(
                    "Documents cannot be released for an inactive request"
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
    }

    /*
     * ---------------------------------------------------------
     * NORMALIZE OPTIONAL STRING
     * ---------------------------------------------------------
     */

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
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
            String performedBy,
            String performedByType,
            String message
    ) {

        EmergencyRequestLog log =
                EmergencyRequestLog.builder()
                        .emergencyRequest(
                                emergencyRequest
                        )
                        .action(
                                action
                        )
                        .previousStatus(
                                previousStatus
                        )
                        .newStatus(
                                newStatus
                        )
                        .performedBy(
                                performedBy
                        )
                        .performedByType(
                                performedByType
                        )
                        .message(
                                message
                        )
                        .build();

        emergencyRequestLogRepository.save(
                log
        );
    }
}