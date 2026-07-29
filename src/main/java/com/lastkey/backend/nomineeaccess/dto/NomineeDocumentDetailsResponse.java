package com.lastkey.backend.nomineeaccess.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomineeDocumentDetailsResponse {

    private UUID releaseHistoryId;

    private UUID documentId;

    private String title;

    private String description;

    private String originalFileName;

    private String fileType;

    private String mimeType;

    private Long fileSize;

    private String categoryName;

    private String ownerName;

    private LocalDate expiryDate;

    private String aiDocumentType;

    private String aiSummary;

    private Boolean canView;

    private Boolean canDownload;

    private Boolean accessRevoked;

    private String revocationReason;

    private LocalDateTime releasedAt;

    private LocalDateTime accessExpiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime lastAccessedAt;

    private Integer downloadCount;
}