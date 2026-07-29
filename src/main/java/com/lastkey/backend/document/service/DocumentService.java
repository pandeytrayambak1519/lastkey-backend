package com.lastkey.backend.document.service;

import com.lastkey.backend.document.dto.request.DocumentSearchRequest;
import com.lastkey.backend.document.dto.request.DocumentUpdateRequest;
import com.lastkey.backend.document.dto.request.DocumentUploadRequest;
import com.lastkey.backend.document.dto.response.DocumentListResponse;
import com.lastkey.backend.document.dto.response.DocumentResponse;
import com.lastkey.backend.document.dto.response.DocumentSearchResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentService {

    DocumentResponse uploadDocument(
            MultipartFile file,
            DocumentUploadRequest request,
            String email
    );

    DocumentResponse getDocumentById(
            UUID documentId,
            String email
    );

    DocumentListResponse getMyDocuments(
            String email,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    DocumentResponse updateDocument(
            UUID documentId,
            DocumentUpdateRequest request,
            String email
    );

    DocumentResponse toggleFavorite(
            UUID documentId,
            String email
    );

    DocumentListResponse getFavoriteDocuments(
            String email,
            int page,
            int size
    );

    DocumentResponse archiveDocument(
            UUID documentId,
            String email
    );

    DocumentResponse restoreDocument(
            UUID documentId,
            String email
    );

    DocumentListResponse getArchivedDocuments(
            String email,
            int page,
            int size
    );

    Resource downloadDocument(
            UUID documentId,
            String email
    );

    DocumentResponse getDocumentForDownload(
            UUID documentId,
            String email
    );

    /*
     * Existing simple keyword search.
     */
    DocumentListResponse searchDocuments(
            String keyword,
            String email,
            int page,
            int size
    );

    /*
     * Advanced search and filter.
     *
     * Supports:
     * - keyword
     * - category
     * - file type
     * - favorite
     * - archived
     * - expiry date range
     * - created date range
     * - pagination
     * - sorting
     */
    DocumentSearchResponse searchDocuments(
            DocumentSearchRequest request,
            String email
    );

    DocumentListResponse getDocumentsByCategory(
            UUID categoryId,
            String email,
            int page,
            int size
    );

    void deleteDocument(
            UUID documentId,
            String email
    );
}