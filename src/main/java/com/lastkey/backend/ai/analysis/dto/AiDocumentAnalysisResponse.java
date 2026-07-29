package com.lastkey.backend.ai.analysis.dto;

import com.lastkey.backend.ai.classification.dto.DocumentClassificationResponse;
import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import com.lastkey.backend.ai.ocr.dto.DocumentOcrResponse;
import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryResponse;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDocumentAnalysisResponse {

    private String originalFileName;

    private String mimeType;

    private Long fileSize;

    private AiDocumentType detectedDocumentType;

    private String detectedDocumentTypeName;

    private String suggestedCategoryName;

    private String summary;

    private Map<String, String> extractedFields;

    private List<String> missingFields;

    /*
     * Combined confidence calculated from:
     *
     * OCR confidence
     * Classification confidence
     * Summary confidence
     */
    private Double overallConfidence;

    private Boolean manualReviewRecommended;

    private String analysisMessage;

    /*
     * Detailed responses are also returned so the frontend
     * can display each analysis stage independently.
     */
    private DocumentOcrResponse ocr;

    private DocumentClassificationResponse classification;

    private AiDocumentSummaryResponse documentSummary;
}