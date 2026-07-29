package com.lastkey.backend.dashboard.dto;

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
public class RecentDocumentResponse {

    private UUID id;

    private String title;

    private String fileType;

    private String categoryName;

    private Boolean favorite;

    private Boolean archived;

    private LocalDate expiryDate;

    private LocalDateTime createdAt;
}