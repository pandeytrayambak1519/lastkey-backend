package com.lastkey.backend.emergency.dto.response;

import com.lastkey.backend.emergency.enums.EmergencyLogAction;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyHistoryResponse {

    private UUID id;

    private UUID emergencyRequestId;

    private EmergencyLogAction action;

    private EmergencyStatus previousStatus;

    private EmergencyStatus newStatus;

    private String performedBy;

    private String performedByType;

    private String message;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime createdAt;
}