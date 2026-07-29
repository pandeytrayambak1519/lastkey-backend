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
public class NomineeAccessibleDocumentResponse {

    private UUID releaseHistoryId;

    private UUID documentId;

    private String title;

    private String description;

    private String originalFileName;

    private String fileType;

    private String mimeType;

    private Long fileSize;

    private String categoryName;

    private LocalDate expiryDate;

    private Boolean canView;

    private Boolean canDownload;

    private Boolean accessRevoked;

    private LocalDateTime releasedAt;

    private LocalDateTime accessExpiresAt;

    private LocalDateTime lastAccessedAt;

    private Integer downloadCount;
}