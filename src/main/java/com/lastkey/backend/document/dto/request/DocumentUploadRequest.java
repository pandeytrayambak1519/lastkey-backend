package com.lastkey.backend.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class DocumentUploadRequest {

    @NotBlank(message = "Document title is required")
    @Size(
            max = 200,
            message = "Title cannot exceed 200 characters"
    )
    private String title;

    @Size(
            max = 1000,
            message = "Description cannot exceed 1000 characters"
    )
    private String description;

    private UUID categoryId;

    private LocalDate expiryDate;
}