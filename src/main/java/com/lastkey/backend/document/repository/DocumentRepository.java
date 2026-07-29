package com.lastkey.backend.document.repository;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository
        extends JpaRepository<Document, UUID>,
        JpaSpecificationExecutor<Document> {

    Optional<Document> findByIdAndOwner(
            UUID id,
            User owner
    );

    Optional<Document> findByIdAndOwnerAndStatusNot(
            UUID id,
            User owner,
            DocumentStatus status
    );

    Page<Document> findByOwnerAndStatus(
            User owner,
            DocumentStatus status,
            Pageable pageable
    );

    List<Document> findByOwnerAndStatus(
            User owner,
            DocumentStatus status
    );

    Page<Document> findByOwnerAndCategoryIdAndStatus(
            User owner,
            UUID categoryId,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByOwnerAndTitleContainingIgnoreCaseAndStatus(
            User owner,
            String title,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByOwnerAndFavoriteTrueAndStatus(
            User owner,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByOwnerAndArchivedTrueAndStatus(
            User owner,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByOwnerAndArchivedFalseAndStatus(
            User owner,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByOwnerAndFileTypeIgnoreCaseAndStatus(
            User owner,
            String fileType,
            DocumentStatus status,
            Pageable pageable
    );

    /*
     * Used by DocumentExpiryScheduler.
     * Returns active documents whose expiry date is inside
     * the scheduler processing window.
     */
    List<Document> findByStatusAndExpiryDateBetween(
            DocumentStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    /*
     * Returns active documents that are already expired and
     * whose final expiry notification has not yet been sent.
     */
    List<Document>
    findByStatusAndExpiryDateLessThanEqualAndExpiryNotificationSentFalse(
            DocumentStatus status,
            LocalDate expiryDate
    );

    boolean existsByStoredFileName(
            String storedFileName
    );

    boolean existsByChecksumAndOwner(
            String checksum,
            User owner
    );

    long countByOwnerAndStatus(
            User owner,
            DocumentStatus status
    );

    long countByOwnerAndFavoriteTrueAndStatus(
            User owner,
            DocumentStatus status
    );

    long countByOwnerAndArchivedTrueAndStatus(
            User owner,
            DocumentStatus status
    );

    /*
     * Dashboard:
     * Counts active documents expiring within the given range.
     */
    long countByOwnerAndStatusAndExpiryDateBetween(
            User owner,
            DocumentStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    /*
     * Dashboard:
     * Returns the latest five active documents.
     */
    List<Document>
    findTop5ByOwnerAndStatusOrderByCreatedAtDesc(
            User owner,
            DocumentStatus status
    );
}