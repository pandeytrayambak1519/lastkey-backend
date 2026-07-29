package com.lastkey.backend.audit.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;

    private String actorEmail;

    private String actorType;

    private String action;

    private String resourceType;

    private String resourceId;

    private String httpMethod;

    private String endpoint;

    private Integer httpStatus;

    private Boolean successful;

    private String ipAddress;

    private Long executionTimeMs;

    private String failureMessage;

    private LocalDateTime createdAt;
}