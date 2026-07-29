package com.lastkey.backend.ai.ocr.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentOcrResponse {

    private String originalFileName;

    private String mimeType;

    private Long fileSize;

    /*
     * PDF_TEXT:
     * Text was available directly inside the PDF.
     *
     * IMAGE_OCR:
     * An uploaded image was processed using OCR.
     *
     * PDF_OCR:
     * PDF pages were converted to images and processed.
     */
    private String extractionMethod;

    private String extractedText;

    private Integer extractedCharacterCount;

    private Integer processedPages;

    private Double confidence;

    private Boolean manualReviewRecommended;

    private String message;
}