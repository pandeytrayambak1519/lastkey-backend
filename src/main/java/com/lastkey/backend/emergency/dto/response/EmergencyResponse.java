package com.lastkey.backend.emergency.dto.response;

import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.enums.EmergencyTriggerType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyResponse {

    private UUID id;

    private UUID ownerId;

    private String ownerName;

    private UUID nomineeId;

    private String nomineeName;

    private String nomineeEmail;

    private EmergencyStatus status;

    private EmergencyTriggerType triggerType;

    private String reason;

    private String evidenceUrl;

    private Integer waitingPeriodDays;

    private LocalDateTime ownerNotifiedAt;

    private LocalDateTime scheduledReleaseAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime releasedAt;

    private LocalDateTime expiredAt;

    private String ownerResponseMessage;

    private String adminReviewMessage;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}