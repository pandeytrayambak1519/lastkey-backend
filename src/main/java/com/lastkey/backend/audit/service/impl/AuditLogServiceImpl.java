package com.lastkey.backend.audit.service.impl;

import com.lastkey.backend.audit.dto.response.AuditLogResponse;
import com.lastkey.backend.audit.entity.AuditLog;
import com.lastkey.backend.audit.mapper.AuditLogMapper;
import com.lastkey.backend.audit.repository.AuditLogRepository;
import com.lastkey.backend.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl
        implements AuditLogService {
	
	private final AuditLogMapper auditLogMapper;

    private final AuditLogRepository auditLogRepository;

    /*
     * The audit record is saved in an independent transaction.
     *
     * Even when the main business transaction fails,
     * its audit information can still be recorded.
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void saveAuditLog(
            AuditLog auditLog
    ) {

        if (auditLog == null) {

            log.warn(
                    "Attempted to save a null audit log"
            );

            return;
        }

        auditLogRepository.save(
                auditLog
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAllLogs(
            Pageable pageable
    ) {

        return auditLogRepository
                .findAll(pageable)
                .map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByActor(
            String actorEmail,
            Pageable pageable
    ) {

        return auditLogRepository
                .findByActorEmailOrderByCreatedAtDesc(
                        actorEmail,
                        pageable
                )
                .map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByResource(
            String resourceType,
            Pageable pageable
    ) {

        return auditLogRepository
                .findByResourceTypeOrderByCreatedAtDesc(
                        resourceType.toUpperCase(),
                        pageable
                )
                .map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {

        return auditLogRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        start,
                        end,
                        pageable
                )
                .map(auditLogMapper::toResponse);
    }
}