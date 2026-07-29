package com.lastkey.backend.document.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class DocumentUpdateRequest {

    @Size(
            min = 1,
            max = 200,
            message = "Title must contain 1 to 200 characters"
    )
    private String title;

    @Size(
            max = 1000,
            message = "Description cannot exceed 1000 characters"
    )
    private String description;

    private UUID categoryId;

    private Boolean favorite;

    private Boolean archived;

    /**
     * Optional expiry date.
     */
    private LocalDate expiryDate;
}