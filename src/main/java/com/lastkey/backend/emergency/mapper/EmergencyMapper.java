package com.lastkey.backend.emergency.mapper;

import com.lastkey.backend.emergency.dto.response.EmergencyResponse;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import org.springframework.stereotype.Component;

@Component
public class EmergencyMapper {

    public EmergencyResponse toResponse(EmergencyRequest request) {

        if (request == null) {
            return null;
        }

        String ownerName = null;

        if (request.getOwner() != null) {
            ownerName = buildFullName(
                    request.getOwner().getFirstName(),
                    request.getOwner().getLastName()
            );
        }

        String nomineeName = null;
        String nomineeEmail = null;

        if (request.getNominee() != null) {
            nomineeName = buildFullName(
                    request.getNominee().getFirstName(),
                    request.getNominee().getLastName()
            );

            nomineeEmail = request.getNominee().getEmail();
        }

        return EmergencyResponse.builder()
                .id(request.getId())
                .ownerId(
                        request.getOwner() != null
                                ? request.getOwner().getId()
                                : null
                )
                .ownerName(ownerName)
                .nomineeId(
                        request.getNominee() != null
                                ? request.getNominee().getId()
                                : null
                )
                .nomineeName(nomineeName)
                .nomineeEmail(nomineeEmail)
                .status(request.getStatus())
                .triggerType(request.getTriggerType())
                .reason(request.getReason())
                .evidenceUrl(request.getEvidenceUrl())
                .waitingPeriodDays(request.getWaitingPeriodDays())
                .ownerNotifiedAt(request.getOwnerNotifiedAt())
                .scheduledReleaseAt(request.getScheduledReleaseAt())
                .approvedAt(request.getApprovedAt())
                .rejectedAt(request.getRejectedAt())
                .cancelledAt(request.getCancelledAt())
                .releasedAt(request.getReleasedAt())
                .expiredAt(request.getExpiredAt())
                .ownerResponseMessage(request.getOwnerResponseMessage())
                .adminReviewMessage(request.getAdminReviewMessage())
                .active(request.getActive())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
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