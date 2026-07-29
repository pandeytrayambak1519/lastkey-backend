package com.lastkey.backend.nominee.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NomineeDocumentResponse {

    private UUID accessId;

    private UUID nomineeId;

    private UUID documentId;

    private String documentName;

    private String fileType;

    private Boolean canView;

    private Boolean canDownload;

    private LocalDateTime grantedAt;

    private LocalDateTime updatedAt;
}