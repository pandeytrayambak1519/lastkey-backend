package com.lastkey.backend.ai.summary.controller;

import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryRequest;
import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryResponse;
import com.lastkey.backend.ai.summary.service.AiDocumentSummaryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/document-summary")
public class AiDocumentSummaryController {

    private final AiDocumentSummaryService
            aiDocumentSummaryService;

    public AiDocumentSummaryController(
            AiDocumentSummaryService aiDocumentSummaryService
    ) {
        this.aiDocumentSummaryService =
                aiDocumentSummaryService;
    }

    /*
     * POST /api/v1/ai/document-summary
     */
    @PostMapping
    public ResponseEntity<AiDocumentSummaryResponse>
    generateDocumentSummary(

            @Valid
            @RequestBody
            AiDocumentSummaryRequest request,

            Authentication authentication
    ) {

        validateAuthentication(
                authentication
        );

        AiDocumentSummaryResponse response =
                aiDocumentSummaryService.generateSummary(
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
                || authentication.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Authenticated user information is unavailable"
            );
        }
    }
}