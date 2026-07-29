package com.lastkey.backend.audit.repository;

import com.lastkey.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByActorEmailOrderByCreatedAtDesc(
            String actorEmail,
            Pageable pageable
    );

    Page<AuditLog> findByResourceTypeOrderByCreatedAtDesc(
            String resourceType,
            Pageable pageable
    );

    Page<AuditLog> findBySuccessfulOrderByCreatedAtDesc(
            Boolean successful,
            Pageable pageable
    );

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<AuditLog>
    findByActorEmailAndCreatedAtBetweenOrderByCreatedAtDesc(
            String actorEmail,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}