package com.lastkey.backend.emergency.mapper;

import com.lastkey.backend.emergency.dto.response.EmergencyReleaseResponse;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import org.springframework.stereotype.Component;

@Component
public class EmergencyReleaseMapper {

    public EmergencyReleaseResponse toResponse(
            EmergencyReleaseHistory release
    ) {

        if (release == null) {
            return null;
        }

        String nomineeName = null;

        if (release.getNominee() != null) {
            nomineeName = buildFullName(
                    release.getNominee().getFirstName(),
                    release.getNominee().getLastName()
            );
        }

        String documentName = null;

        if (release.getDocument() != null) {
            documentName = release.getDocument().getTitle();
        }

        return EmergencyReleaseResponse.builder()
                .id(release.getId())
                .emergencyRequestId(
                        release.getEmergencyRequest() != null
                                ? release.getEmergencyRequest().getId()
                                : null
                )
                .nomineeId(
                        release.getNominee() != null
                                ? release.getNominee().getId()
                                : null
                )
                .nomineeName(nomineeName)
                .documentId(
                        release.getDocument() != null
                                ? release.getDocument().getId()
                                : null
                )
                .documentName(documentName)
                .canView(release.getCanView())
                .canDownload(release.getCanDownload())
                .releasedAt(release.getReleasedAt())
                .accessExpiresAt(release.getAccessExpiresAt())
                .accessRevoked(release.getAccessRevoked())
                .revokedAt(release.getRevokedAt())
                .revocationReason(release.getRevocationReason())
                .downloadCount(release.getDownloadCount())
                .lastAccessedAt(release.getLastAccessedAt())
                .build();
    }

    private String buildFullName(
            String firstName,
            String lastName
    ) {

        String first = firstName != null
                ? firstName.trim()
                : "";

        String last = lastName != null
                ? lastName.trim()
                : "";

        return (first + " " + last).trim();
    }
}