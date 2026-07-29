package com.lastkey.backend.nomineeaccess.service.impl;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.document.service.FileStorageService;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.repository.EmergencyReleaseHistoryRepository;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.repository.NomineeRepository;
import com.lastkey.backend.nomineeaccess.dto.*;
import com.lastkey.backend.nomineeaccess.entity.NomineeAccessAudit;
import com.lastkey.backend.nomineeaccess.enums.NomineeAccessAction;
import com.lastkey.backend.nomineeaccess.mapper.NomineeSecureAccessMapper;
import com.lastkey.backend.nomineeaccess.repository.NomineeAccessAuditRepository;
import com.lastkey.backend.nomineeaccess.service.NomineeSecureAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NomineeSecureAccessServiceImpl
        implements NomineeSecureAccessService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NomineeRepository nomineeRepository;

    private final EmergencyReleaseHistoryRepository
            emergencyReleaseHistoryRepository;

    private final NomineeAccessAuditRepository
            nomineeAccessAuditRepository;

    private final FileStorageService fileStorageService;

    private final NomineeSecureAccessMapper mapper;

    @Override
    @Transactional
    public List<NomineeAccessibleDocumentResponse>
    getAccessibleDocuments(
            String nomineeEmail,
            String ipAddress,
            String userAgent
    ) {

        Nominee nominee =
                getAuthenticatedNominee(nomineeEmail);

        List<EmergencyReleaseHistory> releases =
                emergencyReleaseHistoryRepository
                        .findByNomineeOrderByReleasedAtDesc(
                                nominee
                        );

        saveAudit(
                nominee,
                null,
                null,
                NomineeAccessAction.DOCUMENT_LIST_VIEWED,
                true,
                null,
                ipAddress,
                userAgent
        );

        return releases.stream()
                .filter(this::isCurrentlyAccessible)
                .filter(release ->
                        isDocumentActive(
                                release.getDocument()
                        )
                )
                .map(
                        mapper::toAccessibleDocumentResponse
                )
                .toList();
    }

    @Override
    @Transactional
    public NomineeDocumentDetailsResponse
    getDocumentDetails(
            UUID documentId,
            String nomineeEmail,
            String ipAddress,
            String userAgent
    ) {

        Nominee nominee =
                getAuthenticatedNominee(nomineeEmail);

        EmergencyReleaseHistory release =
                getRelease(nominee, documentId);

        try {

            validateGeneralAccess(release);

            if (!Boolean.TRUE.equals(
                    release.getCanView()
            )) {

                throw new SecurityException(
                        "View permission is not enabled"
                );
            }

            updateLastAccessed(release);

            saveAudit(
                    nominee,
                    release.getDocument(),
                    release,
                    NomineeAccessAction
                            .DOCUMENT_DETAILS_VIEWED,
                    true,
                    null,
                    ipAddress,
                    userAgent
            );

            return mapper.toDetailsResponse(release);

        } catch (RuntimeException exception) {

            saveDeniedAudit(
                    nominee,
                    release,
                    exception.getMessage(),
                    ipAddress,
                    userAgent
            );

            throw exception;
        }
    }

    @Override
    @Transactional
    public NomineeFileAccessResponse viewDocument(
            UUID documentId,
            String nomineeEmail,
            String ipAddress,
            String userAgent
    ) {

        Nominee nominee =
                getAuthenticatedNominee(nomineeEmail);

        EmergencyReleaseHistory release =
                getRelease(nominee, documentId);

        try {

            validateGeneralAccess(release);

            if (!Boolean.TRUE.equals(
                    release.getCanView()
            )) {

                throw new SecurityException(
                        "View permission is not enabled"
                );
            }

            Document document =
                    release.getDocument();

            Resource resource =
                    loadDocumentResource(document);

            updateLastAccessed(release);

            saveAudit(
                    nominee,
                    document,
                    release,
                    NomineeAccessAction.DOCUMENT_VIEWED,
                    true,
                    null,
                    ipAddress,
                    userAgent
            );

            return buildFileResponse(
                    document,
                    resource
            );

        } catch (RuntimeException exception) {

            saveDeniedAudit(
                    nominee,
                    release,
                    exception.getMessage(),
                    ipAddress,
                    userAgent
            );

            throw exception;
        }
    }

    @Override
    @Transactional
    public NomineeFileAccessResponse downloadDocument(
            UUID documentId,
            String nomineeEmail,
            String ipAddress,
            String userAgent
    ) {

        Nominee nominee =
                getAuthenticatedNominee(nomineeEmail);

        EmergencyReleaseHistory release =
                getRelease(nominee, documentId);

        try {

            validateGeneralAccess(release);

            if (!Boolean.TRUE.equals(
                    release.getCanDownload()
            )) {

                throw new SecurityException(
                        "Download permission is not enabled"
                );
            }

            Document document =
                    release.getDocument();

            Resource resource =
                    loadDocumentResource(document);

            int currentDownloadCount =
                    release.getDownloadCount() == null
                            ? 0
                            : release.getDownloadCount();

            release.setDownloadCount(
                    currentDownloadCount + 1
            );

            updateLastAccessed(release);

            saveAudit(
                    nominee,
                    document,
                    release,
                    NomineeAccessAction
                            .DOCUMENT_DOWNLOADED,
                    true,
                    null,
                    ipAddress,
                    userAgent
            );

            return buildFileResponse(
                    document,
                    resource
            );

        } catch (RuntimeException exception) {

            saveDeniedAudit(
                    nominee,
                    release,
                    exception.getMessage(),
                    ipAddress,
                    userAgent
            );

            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NomineeAccessHistoryResponse>
    getAccessHistory(
            String nomineeEmail,
            int page,
            int size
    ) {

        Nominee nominee =
                getAuthenticatedNominee(nomineeEmail);

        int safePage = Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        MAX_PAGE_SIZE
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        return nomineeAccessAuditRepository
                .findByNomineeOrderByCreatedAtDesc(
                        nominee,
                        pageable
                )
                .map(mapper::toHistoryResponse);
    }

    private Nominee getAuthenticatedNominee(
            String email
    ) {

        if (email == null || email.isBlank()) {

            throw new SecurityException(
                    "Authenticated nominee email is required"
            );
        }

        Nominee nominee =
                nomineeRepository
                        .findFirstByEmailIgnoreCaseAndActiveTrue(
                                email.trim()
                        )
                        .orElseThrow(() ->
                                new SecurityException(
                                        "Active nominee account was not found"
                                )
                        );

        if (!Boolean.TRUE.equals(
                nominee.getActive()
        )) {

            throw new SecurityException(
                    "Nominee account is inactive"
            );
        }

        if (!Boolean.TRUE.equals(
                nominee.getEmailVerified()
        )) {

            throw new SecurityException(
                    "Nominee email is not verified"
            );
        }

        return nominee;
    }

    private EmergencyReleaseHistory getRelease(
            Nominee nominee,
            UUID documentId
    ) {

        if (documentId == null) {

            throw new IllegalArgumentException(
                    "Document ID is required"
            );
        }

        return emergencyReleaseHistoryRepository
                .findFirstByNomineeAndDocumentIdOrderByReleasedAtDesc(
                        nominee,
                        documentId
                )
                .orElseThrow(() ->
                        new SecurityException(
                                "Document is not released to this nominee"
                        )
                );
    }

    private void validateGeneralAccess(
            EmergencyReleaseHistory release
    ) {

        if (release == null) {

            throw new SecurityException(
                    "Emergency release was not found"
            );
        }

        if (Boolean.TRUE.equals(
                release.getAccessRevoked()
        )) {

            throw new SecurityException(
                    "Document access has been revoked"
            );
        }

        LocalDateTime expiresAt =
                release.getAccessExpiresAt();

        if (expiresAt != null
                && !expiresAt.isAfter(
                        LocalDateTime.now()
                )) {

            release.setAccessRevoked(true);
            release.setCanView(false);
            release.setCanDownload(false);
            release.setRevokedAt(
                    LocalDateTime.now()
            );
            release.setRevocationReason(
                    "Nominee access expired automatically"
            );

            emergencyReleaseHistoryRepository.save(
                    release
            );

            throw new SecurityException(
                    "Document access has expired"
            );
        }

        if (!isDocumentActive(
                release.getDocument()
        )) {

            throw new SecurityException(
                    "Document is no longer active"
            );
        }
    }

    private boolean isCurrentlyAccessible(
            EmergencyReleaseHistory release
    ) {

        if (release == null) {
            return false;
        }

        if (Boolean.TRUE.equals(
                release.getAccessRevoked()
        )) {
            return false;
        }

        if (release.getAccessExpiresAt() != null
                && !release.getAccessExpiresAt()
                .isAfter(LocalDateTime.now())) {

            return false;
        }

        return Boolean.TRUE.equals(
                release.getCanView()
        ) || Boolean.TRUE.equals(
                release.getCanDownload()
        );
    }

    private boolean isDocumentActive(
            Document document
    ) {

        return document != null
                && document.getStatus()
                == DocumentStatus.ACTIVE;
    }

    private Resource loadDocumentResource(
            Document document
    ) {

        if (document.getOwner() == null
                || document.getOwner().getId() == null) {

            throw new IllegalStateException(
                    "Document owner information is missing"
            );
        }

        String ownerFolder =
                document.getOwner()
                        .getId()
                        .toString();

        Resource resource =
                fileStorageService.loadFileAsResource(
                        document.getStoredFileName(),
                        ownerFolder
                );

        if (resource == null
                || !resource.exists()
                || !resource.isReadable()) {

            throw new IllegalStateException(
                    "Document file is unavailable"
            );
        }

        return resource;
    }

    private void updateLastAccessed(
            EmergencyReleaseHistory release
    ) {

        release.setLastAccessedAt(
                LocalDateTime.now()
        );

        emergencyReleaseHistoryRepository.save(
                release
        );
    }

    private NomineeFileAccessResponse
    buildFileResponse(
            Document document,
            Resource resource
    ) {

        return NomineeFileAccessResponse.builder()
                .resource(resource)
                .fileName(
                        document.getOriginalFileName()
                )
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .build();
    }

    private void saveDeniedAudit(
            Nominee nominee,
            EmergencyReleaseHistory release,
            String reason,
            String ipAddress,
            String userAgent
    ) {

        saveAudit(
                nominee,
                release != null
                        ? release.getDocument()
                        : null,
                release,
                NomineeAccessAction.ACCESS_DENIED,
                false,
                reason,
                ipAddress,
                userAgent
        );
    }

    private void saveAudit(
            Nominee nominee,
            Document document,
            EmergencyReleaseHistory release,
            NomineeAccessAction action,
            boolean successful,
            String failureReason,
            String ipAddress,
            String userAgent
    ) {

        NomineeAccessAudit audit =
                NomineeAccessAudit.builder()
                        .nominee(nominee)
                        .document(document)
                        .releaseHistory(release)
                        .action(action)
                        .successful(successful)
                        .failureReason(failureReason)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .build();

        nomineeAccessAuditRepository.save(
                audit
        );
    }
}