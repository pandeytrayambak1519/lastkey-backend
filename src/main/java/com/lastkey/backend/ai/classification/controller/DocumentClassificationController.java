package com.lastkey.backend.ai.classification.controller;

import com.lastkey.backend.ai.classification.dto.DocumentClassificationRequest;
import com.lastkey.backend.ai.classification.dto.DocumentClassificationResponse;
import com.lastkey.backend.ai.classification.service.DocumentClassificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/document-classification")
@RequiredArgsConstructor
public class DocumentClassificationController {

    private final DocumentClassificationService
            documentClassificationService;

    /*
     * POST /api/v1/ai/document-classification
     */
    @PostMapping
    public ResponseEntity<DocumentClassificationResponse>
    classifyDocument(

            @Valid
            @RequestBody
            DocumentClassificationRequest request,

            Authentication authentication
    ) {

        validateAuthentication(
                authentication
        );

        DocumentClassificationResponse response =
                documentClassificationService.classify(
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