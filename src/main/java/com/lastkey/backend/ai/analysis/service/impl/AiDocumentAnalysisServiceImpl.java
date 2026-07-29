package com.lastkey.backend.ai.analysis.service.impl;

import com.lastkey.backend.ai.analysis.dto.AiDocumentAnalysisRequest;
import com.lastkey.backend.ai.analysis.dto.AiDocumentAnalysisResponse;
import com.lastkey.backend.ai.analysis.service.AiDocumentAnalysisService;
import com.lastkey.backend.ai.classification.dto.DocumentClassificationRequest;
import com.lastkey.backend.ai.classification.dto.DocumentClassificationResponse;
import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import com.lastkey.backend.ai.classification.service.DocumentClassificationService;
import com.lastkey.backend.ai.ocr.dto.DocumentOcrResponse;
import com.lastkey.backend.ai.ocr.service.DocumentOcrService;
import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryRequest;
import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryResponse;
import com.lastkey.backend.ai.summary.service.AiDocumentSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

@Service
public class AiDocumentAnalysisServiceImpl
        implements AiDocumentAnalysisService {

    private final DocumentOcrService documentOcrService;

    private final DocumentClassificationService
            documentClassificationService;

    private final AiDocumentSummaryService
            aiDocumentSummaryService;

    public AiDocumentAnalysisServiceImpl(
            DocumentOcrService documentOcrService,
            DocumentClassificationService documentClassificationService,
            AiDocumentSummaryService aiDocumentSummaryService
    ) {
        this.documentOcrService =
                documentOcrService;

        this.documentClassificationService =
                documentClassificationService;

        this.aiDocumentSummaryService =
                aiDocumentSummaryService;
    }

    @Override
    public AiDocumentAnalysisResponse analyzeDocument(
            MultipartFile file,
            AiDocumentAnalysisRequest request
    ) {

        validateFile(file);

        AiDocumentAnalysisRequest analysisRequest =
                request == null
                        ? new AiDocumentAnalysisRequest()
                        : request;

        /*
         * Stage 1:
         * Extract text from PDF or image.
         */
        DocumentOcrResponse ocrResponse =
                documentOcrService.extractText(file);

        validateOcrResponse(ocrResponse);

        String originalFileName =
                resolveOriginalFileName(
                        file,
                        ocrResponse
                );

        String title =
                resolveTitle(
                        analysisRequest.getTitle(),
                        originalFileName
                );

        /*
         * Stage 2:
         * Classify the document using metadata and OCR text.
         */
        DocumentClassificationRequest
                classificationRequest =
                DocumentClassificationRequest.builder()
                        .fileName(originalFileName)
                        .title(title)
                        .description(
                                normalizeNullableText(
                                        analysisRequest
                                                .getDescription()
                                )
                        )
                        .extractedText(
                                ocrResponse.getExtractedText()
                        )
                        .build();

        DocumentClassificationResponse
                classificationResponse =
                documentClassificationService.classify(
                        classificationRequest
                );

        AiDocumentType resolvedDocumentType =
                resolveDocumentType(
                        analysisRequest,
                        classificationResponse
                );

        /*
         * Stage 3:
         * Extract structured fields and generate summary.
         */
        AiDocumentSummaryRequest summaryRequest =
                AiDocumentSummaryRequest.builder()
                        .fileName(originalFileName)
                        .title(title)
                        .description(
                                normalizeNullableText(
                                        analysisRequest
                                                .getDescription()
                                )
                        )
                        .documentType(
                                resolvedDocumentType
                        )
                        .extractedText(
                                ocrResponse.getExtractedText()
                        )
                        .build();

        AiDocumentSummaryResponse summaryResponse =
                aiDocumentSummaryService.generateSummary(
                        summaryRequest
                );

        double overallConfidence =
                calculateOverallConfidence(
                        ocrResponse,
                        classificationResponse,
                        summaryResponse,
                        analysisRequest.getDocumentType()
                                != null
                );

        boolean manualReviewRecommended =
                resolveManualReviewRecommendation(
                        ocrResponse,
                        classificationResponse,
                        summaryResponse,
                        overallConfidence
                );

        String analysisMessage =
                createAnalysisMessage(
                        resolvedDocumentType,
                        overallConfidence,
                        manualReviewRecommended,
                        summaryResponse
                );

        return AiDocumentAnalysisResponse.builder()
                .originalFileName(originalFileName)
                .mimeType(ocrResponse.getMimeType())
                .fileSize(ocrResponse.getFileSize())
                .detectedDocumentType(
                        resolvedDocumentType
                )
                .detectedDocumentTypeName(
                        resolveDocumentTypeName(
                                summaryResponse,
                                resolvedDocumentType
                        )
                )
                .suggestedCategoryName(
                        resolveSuggestedCategoryName(
                                classificationResponse
                        )
                )
                .summary(
                        summaryResponse == null
                                ? null
                                : summaryResponse.getSummary()
                )
                .extractedFields(
                        summaryResponse == null
                                || summaryResponse
                                .getExtractedFields() == null
                                ? Collections.emptyMap()
                                : summaryResponse
                                .getExtractedFields()
                )
                .missingFields(
                        summaryResponse == null
                                || summaryResponse
                                .getMissingFields() == null
                                ? Collections.emptyList()
                                : summaryResponse
                                .getMissingFields()
                )
                .overallConfidence(
                        round(overallConfidence)
                )
                .manualReviewRecommended(
                        manualReviewRecommended
                )
                .analysisMessage(
                        analysisMessage
                )
                .ocr(ocrResponse)
                .classification(
                        classificationResponse
                )
                .documentSummary(
                        summaryResponse
                )
                .build();
    }

    private AiDocumentType resolveDocumentType(
            AiDocumentAnalysisRequest request,
            DocumentClassificationResponse classificationResponse
    ) {

        if (request.getDocumentType() != null) {
            return request.getDocumentType();
        }

        if (classificationResponse == null
                || classificationResponse
                .getPredictedType() == null) {

            return AiDocumentType.OTHER;
        }

        return classificationResponse
                .getPredictedType();
    }

    private double calculateOverallConfidence(
            DocumentOcrResponse ocrResponse,
            DocumentClassificationResponse classificationResponse,
            AiDocumentSummaryResponse summaryResponse,
            boolean manuallySelectedType
    ) {

        double ocrConfidence =
                safeConfidence(
                        ocrResponse == null
                                ? null
                                : ocrResponse.getConfidence()
                );

        double classificationConfidence =
                safeConfidence(
                        classificationResponse == null
                                ? null
                                : classificationResponse
                                .getConfidence()
                );

        double summaryConfidence =
                safeConfidence(
                        summaryResponse == null
                                ? null
                                : summaryResponse
                                .getConfidence()
                );

        /*
         * OCR quality is extremely important because classification
         * and field extraction depend on its extracted text.
         */
        double overallConfidence =
                (ocrConfidence * 0.40)
                        + (classificationConfidence * 0.25)
                        + (summaryConfidence * 0.35);

        /*
         * When the user manually selects a document type,
         * classification uncertainty should have less impact.
         */
        if (manuallySelectedType) {

            overallConfidence =
                    (ocrConfidence * 0.45)
                            + (summaryConfidence * 0.50)
                            + (classificationConfidence * 0.05);
        }

        return Math.max(
                0.0,
                Math.min(
                        overallConfidence,
                        100.0
                )
        );
    }

    private boolean resolveManualReviewRecommendation(
            DocumentOcrResponse ocrResponse,
            DocumentClassificationResponse classificationResponse,
            AiDocumentSummaryResponse summaryResponse,
            double overallConfidence
    ) {

        if (overallConfidence < 65.0) {
            return true;
        }

        if (ocrResponse != null
                && Boolean.TRUE.equals(
                ocrResponse
                        .getManualReviewRecommended()
        )) {
            return true;
        }

        if (classificationResponse != null
                && Boolean.TRUE.equals(
                classificationResponse
                        .getManualReviewRecommended()
        )) {
            return true;
        }

        return summaryResponse != null
                && Boolean.TRUE.equals(
                summaryResponse
                        .getManualReviewRecommended()
        );
    }

    private String createAnalysisMessage(
            AiDocumentType documentType,
            double confidence,
            boolean manualReviewRecommended,
            AiDocumentSummaryResponse summaryResponse
    ) {

        if (documentType == AiDocumentType.OTHER) {

            return "Document analysis completed, but the document type "
                    + "could not be identified accurately. "
                    + "Please select a category manually.";
        }

        if (summaryResponse == null
                || summaryResponse.getExtractedFields() == null
                || summaryResponse
                .getExtractedFields()
                .isEmpty()) {

            return "Document analysis completed, but no important "
                    + "structured information was detected.";
        }

        if (manualReviewRecommended) {

            return "Document analysis completed with "
                    + round(confidence)
                    + "% overall confidence. Manual verification "
                    + "is recommended before saving the extracted data.";
        }

        return "Document OCR, classification and information extraction "
                + "completed successfully with "
                + round(confidence)
                + "% overall confidence.";
    }

    private String resolveSuggestedCategoryName(
            DocumentClassificationResponse classificationResponse
    ) {

        if (classificationResponse == null
                || classificationResponse
                .getSuggestedCategoryName() == null
                || classificationResponse
                .getSuggestedCategoryName()
                .isBlank()) {

            return "Other";
        }

        return classificationResponse
                .getSuggestedCategoryName()
                .trim();
    }

    private String resolveDocumentTypeName(
            AiDocumentSummaryResponse summaryResponse,
            AiDocumentType documentType
    ) {

        if (summaryResponse != null
                && summaryResponse.getDocumentTypeName()
                != null
                && !summaryResponse
                .getDocumentTypeName()
                .isBlank()) {

            return summaryResponse
                    .getDocumentTypeName()
                    .trim();
        }

        return documentType == null
                ? "Other Document"
                : convertEnumToDisplayName(
                        documentType
                );
    }

    private String convertEnumToDisplayName(
            AiDocumentType documentType
    ) {

        String normalized =
                documentType.name()
                        .toLowerCase()
                        .replace('_', ' ');

        return Character.toUpperCase(
                normalized.charAt(0)
        )
                + normalized.substring(1)
                + " Document";
    }

    private String resolveTitle(
            String requestedTitle,
            String originalFileName
    ) {

        String normalizedTitle =
                normalizeNullableText(
                        requestedTitle
                );

        if (normalizedTitle != null) {
            return normalizedTitle;
        }

        if (originalFileName == null
                || originalFileName.isBlank()) {

            return "Uploaded Document";
        }

        int dotIndex =
                originalFileName.lastIndexOf('.');

        String title =
                dotIndex > 0
                        ? originalFileName.substring(
                        0,
                        dotIndex
                )
                        : originalFileName;

        title =
                title.replace('_', ' ')
                        .replace('-', ' ')
                        .replaceAll("\\s+", " ")
                        .trim();

        return title.isBlank()
                ? "Uploaded Document"
                : title;
    }

    private String resolveOriginalFileName(
            MultipartFile file,
            DocumentOcrResponse ocrResponse
    ) {

        if (ocrResponse != null
                && ocrResponse.getOriginalFileName()
                != null
                && !ocrResponse
                .getOriginalFileName()
                .isBlank()) {

            return ocrResponse
                    .getOriginalFileName()
                    .trim();
        }

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            return "document";
        }

        return originalFileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private String normalizeNullableText(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private double safeConfidence(
            Double confidence
    ) {

        if (confidence == null
                || confidence.isNaN()
                || confidence.isInfinite()) {

            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        confidence,
                        100.0
                )
        );
    }

    private void validateFile(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Document file is required"
            );
        }
    }

    private void validateOcrResponse(
            DocumentOcrResponse ocrResponse
    ) {

        if (ocrResponse == null) {

            throw new IllegalStateException(
                    "OCR service did not return a response"
            );
        }

        if (ocrResponse.getExtractedText() == null
                || ocrResponse
                .getExtractedText()
                .isBlank()) {

            throw new IllegalStateException(
                    "No readable text was detected in the document"
            );
        }
    }

    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}