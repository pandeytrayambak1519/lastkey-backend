package com.lastkey.backend.ai.analysis.controller;

import com.lastkey.backend.ai.analysis.dto.AiDocumentAnalysisRequest;
import com.lastkey.backend.ai.analysis.dto.AiDocumentAnalysisResponse;
import com.lastkey.backend.ai.analysis.service.AiDocumentAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai/document-analysis")
public class AiDocumentAnalysisController {

    private final AiDocumentAnalysisService
            aiDocumentAnalysisService;

    public AiDocumentAnalysisController(
            AiDocumentAnalysisService aiDocumentAnalysisService
    ) {
        this.aiDocumentAnalysisService =
                aiDocumentAnalysisService;
    }

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AiDocumentAnalysisResponse>
    analyzeDocument(

            @RequestPart("file")
            MultipartFile file,

            @Valid
            @RequestPart(
                    value = "request",
                    required = false
            )
            AiDocumentAnalysisRequest request,

            Authentication authentication
    ) {

        validateAuthentication(
                authentication
        );

        AiDocumentAnalysisResponse response =
                aiDocumentAnalysisService
                        .analyzeDocument(
                                file,
                                request
                        );

        return ResponseEntity.ok(
                response
        );
    }

    private void validateAuthentication(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication
                .getName()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "Authenticated user information is unavailable"
            );
        }
    }
}