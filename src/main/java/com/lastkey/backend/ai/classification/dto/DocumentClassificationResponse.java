package com.lastkey.backend.ai.classification.dto;

import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentClassificationResponse {

    private AiDocumentType predictedType;

    /*
     * Human-readable category name that can later be mapped
     * to the existing Category entity.
     */
    private String suggestedCategoryName;

    /*
     * Confidence between 0 and 100.
     */
    private Double confidence;

    /*
     * Keywords responsible for the prediction.
     */
    private List<String> matchedKeywords;

    /*
     * Score received by each detected category.
     */
    private Map<String, Integer> categoryScores;

    private Boolean manualReviewRecommended;

    private String explanation;
}