package com.lastkey.backend.document.service.impl;

import com.lastkey.backend.category.entity.Category;
import com.lastkey.backend.category.repository.CategoryRepository;
import com.lastkey.backend.document.dto.request.DocumentSearchRequest;
import com.lastkey.backend.document.dto.request.DocumentUpdateRequest;
import com.lastkey.backend.document.dto.request.DocumentUploadRequest;
import com.lastkey.backend.document.dto.response.DocumentListResponse;
import com.lastkey.backend.document.dto.response.DocumentResponse;
import com.lastkey.backend.document.dto.response.DocumentSearchResponse;
import com.lastkey.backend.document.dto.storage.StorageResult;
import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.entity.DocumentAccessLog;
import com.lastkey.backend.document.enums.AccessAction;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.document.exception.DocumentNotFoundException;
import com.lastkey.backend.document.mapper.DocumentMapper;
import com.lastkey.backend.document.repository.DocumentAccessLogRepository;
import com.lastkey.backend.document.repository.DocumentRepository;
import com.lastkey.backend.document.service.DocumentService;
import com.lastkey.backend.document.service.FileStorageService;
import com.lastkey.backend.document.specification.DocumentSpecification;
import com.lastkey.backend.notification.service.NotificationEventService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title",
            "createdAt",
            "updatedAt",
            "fileSize",
            "expiryDate"
    );

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;
    private final FileStorageService fileStorageService;
    private final NotificationEventService notificationEventService;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            DocumentAccessLogRepository accessLogRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            DocumentMapper documentMapper,
            FileStorageService fileStorageService,
            NotificationEventService notificationEventService
    ) {
        this.documentRepository = documentRepository;
        this.accessLogRepository = accessLogRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.documentMapper = documentMapper;
        this.fileStorageService = fileStorageService;
        this.notificationEventService = notificationEventService;
    }

    // =========================================================
    // Upload document
    // =========================================================

    @Override
    public DocumentResponse uploadDocument(
            MultipartFile file,
            DocumentUploadRequest request,
            String email
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Document upload details are required"
            );
        }

        User user = getUser(email);

        Category category = null;

        if (request.getCategoryId() != null) {
            category = getAccessibleCategory(
                    request.getCategoryId(),
                    user
            );
        }

        String mimeType =
                fileStorageService.getMimeType(file);

        String checksum =
                fileStorageService.calculateChecksum(file);

        if (documentRepository.existsByChecksumAndOwner(
                checksum,
                user
        )) {
            throw new IllegalArgumentException(
                    "This document has already been uploaded"
            );
        }

        String ownerFolder = user.getId().toString();

        StorageResult storageResult =
                fileStorageService.storeFile(
                        file,
                        ownerFolder
                );

        String storedFileName =
                storageResult.getStoredFileName();

        try {
            Document document = Document.builder()
                    .title(normalizeRequiredText(
                            request.getTitle(),
                            "Document title is required"
                    ))
                    .description(
                            normalizeNullableText(
                                    request.getDescription()
                            )
                    )
                    .originalFileName(
                            storageResult.getOriginalFileName() != null
                                    ? storageResult.getOriginalFileName()
                                    : getOriginalFileName(file)
                    )
                    .storedFileName(storedFileName)
                    .fileType(
                            getFileType(
                                    storageResult.getOriginalFileName() != null
                                            ? storageResult.getOriginalFileName()
                                            : getOriginalFileName(file),
                                    storageResult.getMimeType() != null
                                            ? storageResult.getMimeType()
                                            : mimeType
                            )
                    )
                    .mimeType(
                            storageResult.getMimeType() != null
                                    ? storageResult.getMimeType()
                                    : mimeType
                    )
                    .fileSize(
                            storageResult.getOriginalSize() != null
                                    ? storageResult.getOriginalSize()
                                    : file.getSize()
                    )
                    .storagePath(ownerFolder)
                    .checksum(
                            storageResult.getOriginalChecksum() != null
                                    ? storageResult.getOriginalChecksum()
                                    : checksum
                    )
                    .encrypted(storageResult.isEncrypted())
                    .encryptionAlgorithm(storageResult.getEncryptionAlgorithm())
                    .encryptionKeyVersion(
                            storageResult.getEncryptionKeyVersion() != null
                                    ? String.valueOf(storageResult.getEncryptionKeyVersion())
                                    : null
                    )
                    .initializationVector(storageResult.getInitializationVector())
                    .encryptedChecksum(storageResult.getEncryptedChecksum())
                    .favorite(false)
                    .archived(false)
                    .status(DocumentStatus.ACTIVE)
                    .expiryDate(request.getExpiryDate())
                    .owner(user)
                    .category(category)
                    .build();

            Document savedDocument =
                    documentRepository.save(document);

            saveAccessLog(
                    savedDocument,
                    user,
                    AccessAction.UPLOAD
            );

            notificationEventService.documentUploaded(
                    savedDocument
            );

            return documentMapper.toResponse(savedDocument);

        } catch (RuntimeException exception) {
            fileStorageService.deleteFile(
                    storedFileName,
                    ownerFolder
            );

            throw exception;
        }
    }

    // =========================================================
    // Get document
    // =========================================================

    @Override
    public DocumentResponse getDocumentById(
            UUID documentId,
            String email
    ) {
        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        saveAccessLog(
                document,
                user,
                AccessAction.VIEW
        );

        return documentMapper.toResponse(document);
    }

    // =========================================================
    // Get active documents
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public DocumentListResponse getMyDocuments(
            String email,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        User user = getUser(email);

        Pageable pageable = createPageable(
                page,
                size,
                sortBy,
                sortDirection
        );

        Page<Document> documentPage =
                documentRepository.findByOwnerAndStatus(
                        user,
                        DocumentStatus.ACTIVE,
                        pageable
                );

        return buildListResponse(documentPage);
    }

    // =========================================================
    // Update document metadata
    // =========================================================

    @Override
    public DocumentResponse updateDocument(
            UUID documentId,
            DocumentUpdateRequest request,
            String email
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Document update details are required"
            );
        }

        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        if (request.getTitle() != null) {
            document.setTitle(
                    normalizeRequiredText(
                            request.getTitle(),
                            "Document title cannot be blank"
                    )
            );
        }

        if (request.getDescription() != null) {
            document.setDescription(
                    normalizeNullableText(
                            request.getDescription()
                    )
            );
        }

        if (request.getCategoryId() != null) {
            Category category = getAccessibleCategory(
                    request.getCategoryId(),
                    user
            );

            document.setCategory(category);
        }

        if (request.getFavorite() != null) {
            document.setFavorite(request.getFavorite());
        }

        if (request.getArchived() != null) {
            applyArchiveState(
                    document,
                    request.getArchived()
            );
        }

        if (request.getExpiryDate() != null) {
            document.setExpiryDate(
                    request.getExpiryDate()
            );
        }

        Document updatedDocument =
                documentRepository.save(document);

        saveAccessLog(
                updatedDocument,
                user,
                AccessAction.UPDATE
        );

        notificationEventService.documentUpdated(
                updatedDocument
        );

        return documentMapper.toResponse(updatedDocument);
    }

    // =========================================================
    // Favorite / unfavorite
    // =========================================================

    @Override
    public DocumentResponse toggleFavorite(
            UUID documentId,
            String email
    ) {
        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        boolean currentlyFavorite =
                Boolean.TRUE.equals(
                        document.getFavorite()
                );

        document.setFavorite(!currentlyFavorite);

        Document updatedDocument =
                documentRepository.save(document);

        saveAccessLog(
                updatedDocument,
                user,
                currentlyFavorite
                        ? AccessAction.UNMARK_FAVORITE
                        : AccessAction.MARK_FAVORITE
        );

        return documentMapper.toResponse(updatedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentListResponse getFavoriteDocuments(
            String email,
            int page,
            int size
    ) {
        User user = getUser(email);

        Pageable pageable = createDefaultPageable(
                page,
                size
        );

        Page<Document> documentPage =
                documentRepository
                        .findByOwnerAndFavoriteTrueAndStatus(
                                user,
                                DocumentStatus.ACTIVE,
                                pageable
                        );

        return buildListResponse(documentPage);
    }

    // =========================================================
    // Archive document
    // =========================================================

    @Override
    public DocumentResponse archiveDocument(
            UUID documentId,
            String email
    ) {
        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        applyArchiveState(document, true);

        Document updatedDocument =
                documentRepository.save(document);

        saveAccessLog(
                updatedDocument,
                user,
                AccessAction.ARCHIVE
        );

        return documentMapper.toResponse(updatedDocument);
    }

    // =========================================================
    // Restore document
    // =========================================================

    @Override
    public DocumentResponse restoreDocument(
            UUID documentId,
            String email
    ) {
        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        applyArchiveState(document, false);

        Document updatedDocument =
                documentRepository.save(document);

        saveAccessLog(
                updatedDocument,
                user,
                AccessAction.RESTORE
        );

        return documentMapper.toResponse(updatedDocument);
    }

    // =========================================================
    // Get archived documents
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public DocumentListResponse getArchivedDocuments(
            String email,
            int page,
            int size
    ) {
        User user = getUser(email);

        Pageable pageable = createDefaultPageable(
                page,
                size
        );

        Page<Document> documentPage =
                documentRepository
                        .findByOwnerAndArchivedTrueAndStatus(
                                user,
                                DocumentStatus.ARCHIVED,
                                pageable
                        );

        return buildListResponse(documentPage);
    }

    // =========================================================
    // Download document
    // =========================================================

    @Override
    public Resource downloadDocument(
            UUID documentId,
            String email
    ) {
        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        Resource resource =
                fileStorageService.loadFileAsResource(
                        document.getStoredFileName(),
                        document.getStoragePath()
                );

        saveAccessLog(
                document,
                user,
                AccessAction.DOWNLOAD
        );

        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentForDownload(
            UUID documentId,
            String email
    ) {
        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        return documentMapper.toResponse(document);
    }

    // =========================================================
    // Search documents
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public DocumentListResponse searchDocuments(
            String keyword,
            String email,
            int page,
            int size
    ) {
        User user = getUser(email);

        String searchKeyword =
                keyword == null
                        ? ""
                        : keyword.trim();

        Pageable pageable = createDefaultPageable(
                page,
                size
        );

        Page<Document> documentPage =
                documentRepository
                        .findByOwnerAndTitleContainingIgnoreCaseAndStatus(
                                user,
                                searchKeyword,
                                DocumentStatus.ACTIVE,
                                pageable
                        );

        return buildListResponse(documentPage);
    }

    // =========================================================
    // Advanced search and filter
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public DocumentSearchResponse searchDocuments(
            DocumentSearchRequest request,
            String email
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Document search request is required"
            );
        }

        User user = getUser(email);

        validateSearchDateRanges(request);

        if (request.getCategoryId() != null) {
            getAccessibleCategory(
                    request.getCategoryId(),
                    user
            );
        }

        Pageable pageable =
                createSearchPageable(request);

        Page<Document> documentPage =
                documentRepository.findAll(
                        DocumentSpecification.build(
                                user,
                                request
                        ),
                        pageable
                );

        return DocumentSearchResponse.builder()
                .documents(
                        documentPage
                                .getContent()
                                .stream()
                                .map(
                                        documentMapper
                                                ::toSearchItemResponse
                                )
                                .toList()
                )
                .currentPage(
                        documentPage.getNumber()
                )
                .pageSize(
                        documentPage.getSize()
                )
                .totalElements(
                        documentPage.getTotalElements()
                )
                .totalPages(
                        documentPage.getTotalPages()
                )
                .first(
                        documentPage.isFirst()
                )
                .last(
                        documentPage.isLast()
                )
                .hasNext(
                        documentPage.hasNext()
                )
                .hasPrevious(
                        documentPage.hasPrevious()
                )
                .build();
    }

    // =========================================================
    // Filter by category
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public DocumentListResponse getDocumentsByCategory(
            UUID categoryId,
            String email,
            int page,
            int size
    ) {
        if (categoryId == null) {
            throw new IllegalArgumentException(
                    "Category ID is required"
            );
        }

        User user = getUser(email);

        getAccessibleCategory(categoryId, user);

        Pageable pageable = createDefaultPageable(
                page,
                size
        );

        Page<Document> documentPage =
                documentRepository
                        .findByOwnerAndCategoryIdAndStatus(
                                user,
                                categoryId,
                                DocumentStatus.ACTIVE,
                                pageable
                        );

        return buildListResponse(documentPage);
    }

    // =========================================================
    // Soft delete document
    // =========================================================

    @Override
    public void deleteDocument(
            UUID documentId,
            String email
    ) {
        User user = getUser(email);

        Document document = getOwnedDocument(
                documentId,
                user
        );

        document.setFavorite(false);
        document.setArchived(false);
        document.setStatus(DocumentStatus.DELETED);

        Document deletedDocument =
                documentRepository.save(document);

        saveAccessLog(
                deletedDocument,
                user,
                AccessAction.DELETE
        );

        notificationEventService.documentDeleted(
                deletedDocument
        );
    }

    // =========================================================
    // User helper
    // =========================================================

    private User getUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required"
            );
        }

        return userRepository
                .findByEmail(email.trim())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Authenticated user was not found"
                        )
                );
    }

    // =========================================================
    // Document helper
    // =========================================================

    private Document getOwnedDocument(
            UUID documentId,
            User user
    ) {
        if (documentId == null) {
            throw new IllegalArgumentException(
                    "Document ID is required"
            );
        }

        return documentRepository
                .findByIdAndOwnerAndStatusNot(
                        documentId,
                        user,
                        DocumentStatus.DELETED
                )
                .orElseThrow(
                        () -> new DocumentNotFoundException(
                                "Document not found with id: "
                                        + documentId
                        )
                );
    }

    // =========================================================
    // Category helper
    // =========================================================

    private Category getAccessibleCategory(
            UUID categoryId,
            User user
    ) {
        if (categoryId == null) {
            throw new IllegalArgumentException(
                    "Category ID is required"
            );
        }

        return categoryRepository
                .findByIdAndOwner(categoryId, user)
                .orElseGet(
                        () -> categoryRepository
                                .findByIdAndSystemCategoryTrue(
                                        categoryId
                                )
                                .orElseThrow(
                                        () -> new IllegalArgumentException(
                                                "Category not found or inaccessible"
                                        )
                                )
                );
    }

    // =========================================================
    // Access log helper
    // =========================================================

    private void saveAccessLog(
            Document document,
            User user,
            AccessAction action
    ) {
        DocumentAccessLog accessLog =
                DocumentAccessLog.builder()
                        .document(document)
                        .user(user)
                        .action(action)
                        .successful(true)
                        .build();

        accessLogRepository.save(accessLog);
    }

    // =========================================================
    // Archive state helper
    // =========================================================

    private void applyArchiveState(
            Document document,
            boolean archived
    ) {
        document.setArchived(archived);

        document.setStatus(
                archived
                        ? DocumentStatus.ARCHIVED
                        : DocumentStatus.ACTIVE
        );
    }

    // =========================================================
    // Advanced search helpers
    // =========================================================

    private Pageable createSearchPageable(
            DocumentSearchRequest request
    ) {
        int page =
                request.getPage() != null
                        ? request.getPage()
                        : 0;

        int size =
                request.getSize() != null
                        ? request.getSize()
                        : DEFAULT_PAGE_SIZE;

        String sortProperty =
                getSearchSortProperty(request);

        Sort.Direction direction =
                request.getSortDirection() != null
                        ? request.getSortDirection()
                        : Sort.Direction.DESC;

        Sort sort = Sort.by(
                direction,
                sortProperty
        );

        /*
         * Secondary sorting gives stable pagination when two
         * documents have the same value in the selected field.
         */
        if (!"id".equals(sortProperty)) {
            sort = sort.and(
                    Sort.by(
                            Sort.Direction.ASC,
                            "id"
                    )
            );
        }

        return PageRequest.of(
                validatePage(page),
                validateSize(size),
                sort
        );
    }

    private String getSearchSortProperty(
            DocumentSearchRequest request
    ) {
        if (request.getSortBy() == null) {
            return "createdAt";
        }

        return switch (request.getSortBy()) {
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case TITLE -> "title";
            case EXPIRY_DATE -> "expiryDate";
            case FILE_SIZE -> "fileSize";
        };
    }

    private void validateSearchDateRanges(
            DocumentSearchRequest request
    ) {
        if (request.getExpiryFrom() != null
                && request.getExpiryTo() != null
                && request.getExpiryFrom()
                .isAfter(request.getExpiryTo())) {

            throw new IllegalArgumentException(
                    "Expiry start date cannot be after expiry end date"
            );
        }

        if (request.getCreatedFrom() != null
                && request.getCreatedTo() != null
                && request.getCreatedFrom()
                .isAfter(request.getCreatedTo())) {

            throw new IllegalArgumentException(
                    "Created start date cannot be after created end date"
            );
        }
    }

    // =========================================================
    // Pagination helpers
    // =========================================================

    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        int validPage = validatePage(page);
        int validSize = validateSize(size);

        String validSortBy =
                sortBy != null
                        && ALLOWED_SORT_FIELDS.contains(sortBy)
                        ? sortBy
                        : "createdAt";

        Sort sort =
                "asc".equalsIgnoreCase(sortDirection)
                        ? Sort.by(validSortBy).ascending()
                        : Sort.by(validSortBy).descending();

        return PageRequest.of(
                validPage,
                validSize,
                sort
        );
    }

    private Pageable createDefaultPageable(
            int page,
            int size
    ) {
        return PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by("createdAt").descending()
        );
    }

    private int validatePage(int page) {
        return Math.max(page, 0);
    }

    private int validateSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    // =========================================================
    // Response helper
    // =========================================================

    private DocumentListResponse buildListResponse(
            Page<Document> documentPage
    ) {
        return DocumentListResponse.builder()
                .documents(
                        documentPage
                                .getContent()
                                .stream()
                                .map(documentMapper::toResponse)
                                .toList()
                )
                .totalElements(
                        documentPage.getTotalElements()
                )
                .totalPages(
                        documentPage.getTotalPages()
                )
                .currentPage(
                        documentPage.getNumber()
                )
                .pageSize(
                        documentPage.getSize()
                )
                .first(
                        documentPage.isFirst()
                )
                .last(
                        documentPage.isLast()
                )
                .hasNext(
                        documentPage.hasNext()
                )
                .hasPrevious(
                        documentPage.hasPrevious()
                )
                .build();
    }

    // =========================================================
    // Text/file helpers
    // =========================================================

    private String normalizeRequiredText(
            String value,
            String errorMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    errorMessage
            );
        }

        return value.trim();
    }

    private String normalizeNullableText(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }

    private String getOriginalFileName(
            MultipartFile file
    ) {
        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Original file name is missing"
            );
        }

        return originalFileName.trim();
    }

    private String getFileType(
            String originalFileName,
            String mimeType
    ) {
        int dotIndex =
                originalFileName.lastIndexOf('.');

        if (dotIndex >= 0
                && dotIndex < originalFileName.length() - 1) {
            return originalFileName
                    .substring(dotIndex + 1)
                    .toLowerCase(Locale.ROOT);
        }

        return switch (mimeType) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "unknown";
        };
    }
}