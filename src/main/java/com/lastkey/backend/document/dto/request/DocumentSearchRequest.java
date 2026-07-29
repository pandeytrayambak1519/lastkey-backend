package com.lastkey.backend.document.dto.request;

import com.lastkey.backend.document.enums.DocumentSortField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchRequest {

    /*
     * Search in document title.
     */
    private String keyword;

    /*
     * Filter using category ID.
     */
    private UUID categoryId;

    /*
     * Example:
     * pdf, jpg, png, docx
     */
    private String fileType;

    private Boolean favorite;

    private Boolean archived;

    /*
     * Expiry-date range.
     */
    private LocalDate expiryFrom;

    private LocalDate expiryTo;

    /*
     * Document creation-date range.
     */
    private LocalDate createdFrom;

    private LocalDate createdTo;

    @Min(value = 0, message = "Page number cannot be negative")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private DocumentSortField sortBy =
            DocumentSortField.CREATED_AT;

    @Builder.Default
    private Sort.Direction sortDirection =
            Sort.Direction.DESC;
}