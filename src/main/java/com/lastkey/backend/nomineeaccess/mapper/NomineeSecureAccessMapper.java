package com.lastkey.backend.nomineeaccess.mapper;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.nomineeaccess.dto.NomineeAccessHistoryResponse;
import com.lastkey.backend.nomineeaccess.dto.NomineeAccessibleDocumentResponse;
import com.lastkey.backend.nomineeaccess.dto.NomineeDocumentDetailsResponse;
import com.lastkey.backend.nomineeaccess.entity.NomineeAccessAudit;
import org.springframework.stereotype.Component;

@Component
public class NomineeSecureAccessMapper {

    public NomineeAccessibleDocumentResponse
    toAccessibleDocumentResponse(
            EmergencyReleaseHistory release
    ) {

        Document document = release.getDocument();

        return NomineeAccessibleDocumentResponse.builder()
                .releaseHistoryId(release.getId())
                .documentId(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .originalFileName(
                        document.getOriginalFileName()
                )
                .fileType(document.getFileType())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .categoryName(
                        document.getCategory() != null
                                ? document.getCategory().getName()
                                : null
                )
                .expiryDate(document.getExpiryDate())
                .canView(release.getCanView())
                .canDownload(release.getCanDownload())
                .accessRevoked(
                        release.getAccessRevoked()
                )
                .releasedAt(release.getReleasedAt())
                .accessExpiresAt(
                        release.getAccessExpiresAt()
                )
                .lastAccessedAt(
                        release.getLastAccessedAt()
                )
                .downloadCount(
                        release.getDownloadCount()
                )
                .build();
    }

    public NomineeDocumentDetailsResponse
    toDetailsResponse(
            EmergencyReleaseHistory release
    ) {

        Document document = release.getDocument();

        return NomineeDocumentDetailsResponse.builder()
                .releaseHistoryId(release.getId())
                .documentId(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .originalFileName(
                        document.getOriginalFileName()
                )
                .fileType(document.getFileType())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .categoryName(
                        document.getCategory() != null
                                ? document.getCategory().getName()
                                : null
                )
                .ownerName(
                        getOwnerName(document)
                )
                .expiryDate(document.getExpiryDate())
                .aiDocumentType(
                        document.getAiDocumentType()
                )
                .aiSummary(document.getAiSummary())
                .canView(release.getCanView())
                .canDownload(release.getCanDownload())
                .accessRevoked(
                        release.getAccessRevoked()
                )
                .revocationReason(
                        release.getRevocationReason()
                )
                .releasedAt(release.getReleasedAt())
                .accessExpiresAt(
                        release.getAccessExpiresAt()
                )
                .revokedAt(release.getRevokedAt())
                .lastAccessedAt(
                        release.getLastAccessedAt()
                )
                .downloadCount(
                        release.getDownloadCount()
                )
                .build();
    }

    public NomineeAccessHistoryResponse
    toHistoryResponse(
            NomineeAccessAudit audit
    ) {

        return NomineeAccessHistoryResponse.builder()
                .id(audit.getId())
                .documentId(
                        audit.getDocument() != null
                                ? audit.getDocument().getId()
                                : null
                )
                .documentTitle(
                        audit.getDocument() != null
                                ? audit.getDocument().getTitle()
                                : null
                )
                .action(audit.getAction())
                .successful(audit.getSuccessful())
                .failureReason(
                        audit.getFailureReason()
                )
                .ipAddress(audit.getIpAddress())
                .createdAt(audit.getCreatedAt())
                .build();
    }

    private String getOwnerName(
            Document document
    ) {

        if (document.getOwner() == null) {
            return null;
        }

        String firstName =
                document.getOwner().getFirstName();

        String lastName =
                document.getOwner().getLastName();

        return (
                normalize(firstName)
                        + " "
                        + normalize(lastName)
        ).trim();
    }

    private String normalize(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}