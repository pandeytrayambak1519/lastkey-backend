package com.lastkey.backend.audit.service;

import com.lastkey.backend.audit.dto.response.AuditLogResponse;
import com.lastkey.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    void saveAuditLog(
            AuditLog auditLog
    );

    Page<AuditLogResponse> getAllLogs(
            Pageable pageable
    );

    Page<AuditLogResponse> getLogsByActor(
            String actorEmail,
            Pageable pageable
    );

    Page<AuditLogResponse> getLogsByResource(
            String resourceType,
            Pageable pageable
    );

    Page<AuditLogResponse> getLogsBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
}