package com.lastkey.backend.emergency.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyReleaseResponse {

    private UUID id;

    private UUID emergencyRequestId;

    private UUID nomineeId;

    private String nomineeName;

    private UUID documentId;

    private String documentName;

    private Boolean canView;

    private Boolean canDownload;

    private LocalDateTime releasedAt;

    private LocalDateTime accessExpiresAt;

    private Boolean accessRevoked;

    private LocalDateTime revokedAt;

    private String revocationReason;

    private Integer downloadCount;

    private LocalDateTime lastAccessedAt;
}