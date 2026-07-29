package com.lastkey.backend.document.controller;

import com.lastkey.backend.document.dto.request.DocumentSearchRequest;
import com.lastkey.backend.document.dto.request.DocumentUpdateRequest;
import com.lastkey.backend.document.dto.request.DocumentUploadRequest;
import com.lastkey.backend.document.dto.response.DocumentListResponse;
import com.lastkey.backend.document.dto.response.DocumentResponse;
import com.lastkey.backend.document.dto.response.DocumentSearchResponse;
import com.lastkey.backend.document.service.DocumentService;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(
            DocumentService documentService
    ) {
        this.documentService = documentService;
    }

    // =========================================================
    // Upload document
    // =========================================================

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentResponse> uploadDocument(

            @RequestPart("file")
            MultipartFile file,

            @Valid
            @RequestPart("request")
            DocumentUploadRequest request,

            Authentication authentication
    ) {

        DocumentResponse response =
                documentService.uploadDocument(
                        file,
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // Get current user's active documents
    // =========================================================

    @GetMapping
    public ResponseEntity<DocumentListResponse> getMyDocuments(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDirection,

            Authentication authentication
    ) {

        DocumentListResponse response =
                documentService.getMyDocuments(
                        getAuthenticatedEmail(authentication),
                        page,
                        size,
                        sortBy,
                        sortDirection
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Get favorite documents
    // =========================================================

    @GetMapping("/favorites")
    public ResponseEntity<DocumentListResponse> getFavoriteDocuments(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            Authentication authentication
    ) {

        DocumentListResponse response =
                documentService.getFavoriteDocuments(
                        getAuthenticatedEmail(authentication),
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Get archived documents
    // =========================================================

    @GetMapping("/archived")
    public ResponseEntity<DocumentListResponse> getArchivedDocuments(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            Authentication authentication
    ) {

        DocumentListResponse response =
                documentService.getArchivedDocuments(
                        getAuthenticatedEmail(authentication),
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Simple keyword search
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<DocumentListResponse> searchDocuments(

            @RequestParam
            String keyword,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            Authentication authentication
    ) {

        DocumentListResponse response =
                documentService.searchDocuments(
                        keyword,
                        getAuthenticatedEmail(authentication),
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Advanced search and filtering
    // =========================================================

    @PostMapping("/advanced-search")
    public ResponseEntity<DocumentSearchResponse>
    advancedSearchDocuments(

            @RequestBody(required = false)
            DocumentSearchRequest request,

            Authentication authentication
    ) {

        DocumentSearchResponse response =
                documentService.searchDocuments(
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Filter documents by category
    // =========================================================

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<DocumentListResponse>
    getDocumentsByCategory(

            @PathVariable
            UUID categoryId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            Authentication authentication
    ) {

        DocumentListResponse response =
                documentService.getDocumentsByCategory(
                        categoryId,
                        getAuthenticatedEmail(authentication),
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Preview document
    // =========================================================

    @GetMapping("/{documentId}/preview")
    public ResponseEntity<Resource> previewDocument(

            @PathVariable
            UUID documentId,

            Authentication authentication
    ) {

        String email =
                getAuthenticatedEmail(authentication);

        /*
         * Load the document metadata to obtain MIME type,
         * original filename and file size.
         */
        DocumentResponse document =
                documentService.getDocumentForDownload(
                        documentId,
                        email
                );

        /*
         * downloadDocument already returns the decrypted file
         * as a Spring Resource.
         */
        Resource resource =
                documentService.downloadDocument(
                        documentId,
                        email
                );

        MediaType mediaType =
                resolveMediaType(
                        document.getMimeType()
                );

        ContentDisposition contentDisposition =
                ContentDisposition
                        .inline()
                        .filename(
                                getSafeDownloadFileName(
                                        document.getOriginalFileName()
                                ),
                                StandardCharsets.UTF_8
                        )
                        .build();

        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                contentDisposition.toString()
                        )
                        .header(
                                HttpHeaders.CACHE_CONTROL,
                                "no-store, no-cache, must-revalidate"
                        )
                        .header(
                                "Pragma",
                                "no-cache"
                        )
                        .header(
                                "X-Content-Type-Options",
                                "nosniff"
                        );

        if (document.getFileSize() != null
                && document.getFileSize() >= 0) {

            responseBuilder.contentLength(
                    document.getFileSize()
            );
        }

        return responseBuilder.body(resource);
    }

    // =========================================================
    // Get document by ID
    // =========================================================

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> getDocumentById(

            @PathVariable
            UUID documentId,

            Authentication authentication
    ) {

        DocumentResponse response =
                documentService.getDocumentById(
                        documentId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Update document
    // =========================================================

    @PatchMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> updateDocument(

            @PathVariable
            UUID documentId,

            @Valid
            @RequestBody
            DocumentUpdateRequest request,

            Authentication authentication
    ) {

        DocumentResponse response =
                documentService.updateDocument(
                        documentId,
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Favorite or unfavorite document
    // =========================================================

    @PatchMapping("/{documentId}/favorite")
    public ResponseEntity<DocumentResponse> toggleFavorite(

            @PathVariable
            UUID documentId,

            Authentication authentication
    ) {

        DocumentResponse response =
                documentService.toggleFavorite(
                        documentId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Archive document
    // =========================================================

    @PatchMapping("/{documentId}/archive")
    public ResponseEntity<DocumentResponse> archiveDocument(

            @PathVariable
            UUID documentId,

            Authentication authentication
    ) {

        DocumentResponse response =
                documentService.archiveDocument(
                        documentId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Restore archived document
    // =========================================================

    @PatchMapping("/{documentId}/restore")
    public ResponseEntity<DocumentResponse> restoreDocument(

            @PathVariable
            UUID documentId,

            Authentication authentication
    ) {

        DocumentResponse response =
                documentService.restoreDocument(
                        documentId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Download document
    // =========================================================

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(

            @PathVariable
            UUID documentId,

            Authentication authentication
    ) {

        String email =
                getAuthenticatedEmail(authentication);

        DocumentResponse document =
                documentService.getDocumentForDownload(
                        documentId,
                        email
                );

        Resource resource =
                documentService.downloadDocument(
                        documentId,
                        email
                );

        MediaType mediaType =
                resolveMediaType(
                        document.getMimeType()
                );

        ContentDisposition contentDisposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                getSafeDownloadFileName(
                                        document.getOriginalFileName()
                                ),
                                StandardCharsets.UTF_8
                        )
                        .build();

        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                contentDisposition.toString()
                        )
                        .header(
                                HttpHeaders.CACHE_CONTROL,
                                "no-store, no-cache, must-revalidate"
                        )
                        .header(
                                "Pragma",
                                "no-cache"
                        )
                        .header(
                                "X-Content-Type-Options",
                                "nosniff"
                        );

        if (document.getFileSize() != null
                && document.getFileSize() >= 0) {

            responseBuilder.contentLength(
                    document.getFileSize()
            );
        }

        return responseBuilder.body(resource);
    }

    // =========================================================
    // Soft delete document
    // =========================================================

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(

            @PathVariable
            UUID documentId,

            Authentication authentication
    ) {

        documentService.deleteDocument(
                documentId,
                getAuthenticatedEmail(authentication)
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    // =========================================================
    // Authentication helper
    // =========================================================

    private String getAuthenticatedEmail(
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

        return authentication
                .getName()
                .trim();
    }

    // =========================================================
    // Media type helper
    // =========================================================

    private MediaType resolveMediaType(
            String mimeType
    ) {

        if (mimeType == null
                || mimeType.isBlank()) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(
                    mimeType.trim()
            );

        } catch (IllegalArgumentException exception) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    // =========================================================
    // Safe filename helper
    // =========================================================

    private String getSafeDownloadFileName(
            String fileName
    ) {

        if (fileName == null
                || fileName.isBlank()) {

            return "document";
        }

        String safeFileName =
                fileName
                        .replace("\\", "_")
                        .replace("/", "_")
                        .replace("\"", "")
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim();

        return safeFileName.isBlank()
                ? "document"
                : safeFileName;
    }
}