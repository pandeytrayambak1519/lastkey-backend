package com.lastkey.backend.document.dto.response;

import com.lastkey.backend.document.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchItemResponse {

    private UUID id;

    private String title;

    private String description;

    private UUID categoryId;

    private String categoryName;

    private String originalFileName;

    private String fileType;

    private Long fileSize;

    private Boolean favorite;

    private Boolean archived;

    private DocumentStatus status;

    private LocalDate expiryDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}