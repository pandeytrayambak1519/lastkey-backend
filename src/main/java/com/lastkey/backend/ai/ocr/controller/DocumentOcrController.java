package com.lastkey.backend.ai.ocr.controller;

import com.lastkey.backend.ai.ocr.dto.DocumentOcrResponse;
import com.lastkey.backend.ai.ocr.service.DocumentOcrService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai/ocr")
public class DocumentOcrController {

    private final DocumentOcrService
            documentOcrService;

    public DocumentOcrController(
            DocumentOcrService documentOcrService
    ) {
        this.documentOcrService =
                documentOcrService;
    }

    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentOcrResponse>
    extractText(

            @RequestPart("file")
            MultipartFile file,

            Authentication authentication
    ) {

        validateAuthentication(
                authentication
        );

        DocumentOcrResponse response =
                documentOcrService.extractText(
                        file
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