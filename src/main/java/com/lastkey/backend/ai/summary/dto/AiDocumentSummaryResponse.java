package com.lastkey.backend.ai.summary.dto;

import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDocumentSummaryResponse {

    private AiDocumentType documentType;

    private String documentTypeName;

    /*
     * Short human-readable summary.
     */
    private String summary;

    /*
     * Extracted structured information.
     *
     * Example:
     * {
     *   "passportNumber": "Z1234567",
     *   "dateOfBirth": "05/08/2004"
     * }
     */
    private Map<String, String> extractedFields;

    /*
     * Fields that should normally exist for this document
     * but were not detected.
     */
    private List<String> missingFields;

    /*
     * Confidence between 0 and 100.
     */
    private Double confidence;

    private Boolean manualReviewRecommended;

    private Integer totalDetectedFields;

    private String extractionMessage;
}