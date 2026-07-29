package com.lastkey.backend.ai.summary.dto;

import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDocumentSummaryRequest {

    @Size(max = 500)
    private String fileName;

    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    /*
     * Optional document type.
     *
     * When not provided, the classification service
     * automatically identifies the document type.
     */
    private AiDocumentType documentType;

    /*
     * Text extracted from the uploaded PDF or image
     * using an OCR service.
     */
    @NotBlank(message = "Extracted document text is required")
    @Size(
            max = 50000,
            message = "Extracted document text cannot exceed 50000 characters"
    )
    private String extractedText;
}