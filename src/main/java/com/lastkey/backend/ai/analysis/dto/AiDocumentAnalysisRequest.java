package com.lastkey.backend.ai.analysis.dto;

import com.lastkey.backend.ai.classification.enums.AiDocumentType;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDocumentAnalysisRequest {

    /*
     * Optional custom document title.
     *
     * When not supplied, the original filename is used.
     */
    @Size(
            max = 255,
            message = "Document title cannot exceed 255 characters"
    )
    private String title;

    @Size(
            max = 2000,
            message = "Document description cannot exceed 2000 characters"
    )
    private String description;

    /*
     * Optional manually selected document type.
     *
     * When null, AI classification automatically detects it.
     */
    private AiDocumentType documentType;
}