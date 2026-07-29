package com.lastkey.backend.document.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    /*
     * =========================================================
     * BASIC INFORMATION
     * =========================================================
     */

    private UUID id;

    private String title;

    private String description;

    private String originalFileName;

    private String fileType;

    private String mimeType;

    private Long fileSize;

    private String checksum;

    /*
     * =========================================================
     * DOCUMENT FLAGS
     * =========================================================
     */

    private Boolean encrypted;

    private Boolean favorite;

    private Boolean archived;

    private String status;

    /*
     * =========================================================
     * CATEGORY INFORMATION
     * =========================================================
     */

    private UUID categoryId;

    private String categoryName;

    /*
     * AI Suggested Category
     */
    private String aiSuggestedCategory;

    /*
     * =========================================================
     * OWNER
     * =========================================================
     */

    private UUID ownerId;

    /*
     * =========================================================
     * EXPIRY
     * =========================================================
     */

    private LocalDate expiryDate;

    /*
     * =========================================================
     * AI DOCUMENT ANALYSIS
     * =========================================================
     */

    /*
     * AI detected document type.
     */
    private String aiDocumentType;

    /*
     * AI generated summary.
     */
    private String aiSummary;

    /*
     * OCR extracted text.
     */
    private String aiExtractedText;

    /*
     * Structured fields extracted by AI.
     *
     * Example:
     *
     * {
     *   "passportNumber":"A1234567",
     *   "name":"John Doe"
     * }
     */
    private Map<String, Object> extractedFields;

    /*
     * Final confidence score.
     *
     * Range:
     * 0.0 - 1.0
     */
    private Double aiConfidence;

    /*
     * Indicates manual verification is recommended.
     */
    private Boolean aiReviewRequired;

    /*
     * =========================================================
     * AUDIT
     * =========================================================
     */

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}