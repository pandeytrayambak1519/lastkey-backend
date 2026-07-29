package com.lastkey.backend.ai.classification.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentClassificationRequest {

    @Size(max = 500)
    private String fileName;

    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    /*
     * Text extracted through OCR can be provided here.
     * It is optional until the OCR module is integrated.
     */
    @Size(max = 20000)
    private String extractedText;
}