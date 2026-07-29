package com.lastkey.backend.audit.mapper;

import com.lastkey.backend.audit.dto.response.AuditLogResponse;
import com.lastkey.backend.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(
            AuditLog auditLog
    ) {

        if (auditLog == null) {
            return null;
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorEmail(auditLog.getActorEmail())
                .actorType(auditLog.getActorType())
                .action(auditLog.getAction())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .httpMethod(auditLog.getHttpMethod())
                .endpoint(auditLog.getEndpoint())
                .httpStatus(auditLog.getHttpStatus())
                .successful(auditLog.getSuccessful())
                .ipAddress(auditLog.getIpAddress())
                .executionTimeMs(
                        auditLog.getExecutionTimeMs()
                )
                .failureMessage(
                        auditLog.getFailureMessage()
                )
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}