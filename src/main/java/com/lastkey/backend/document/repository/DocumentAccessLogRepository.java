package com.lastkey.backend.document.repository;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.entity.DocumentAccessLog;
import com.lastkey.backend.document.enums.AccessAction;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DocumentAccessLogRepository
        extends JpaRepository<DocumentAccessLog, UUID> {

    Page<DocumentAccessLog> findByDocumentOrderByAccessedAtDesc(
            Document document,
            Pageable pageable
    );

    Page<DocumentAccessLog> findByUserOrderByAccessedAtDesc(
            User user,
            Pageable pageable
    );

    Page<DocumentAccessLog> findByUserAndActionOrderByAccessedAtDesc(
            User user,
            AccessAction action,
            Pageable pageable
    );

    Page<DocumentAccessLog> findByDocumentAndActionOrderByAccessedAtDesc(
            Document document,
            AccessAction action,
            Pageable pageable
    );

    List<DocumentAccessLog> findByDocumentAndAccessedAtBetween(
            Document document,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<DocumentAccessLog> findByUserAndAccessedAtBetween(
            User user,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    long countByDocument(
            Document document
    );

    long countByDocumentAndAction(
            Document document,
            AccessAction action
    );

    long countByUserAndAccessedAtBetween(
            User user,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    long countByUserAndAction(
            User user,
            AccessAction action
    );

    void deleteByDocument(
            Document document
    );
}