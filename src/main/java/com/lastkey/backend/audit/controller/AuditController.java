package com.lastkey.backend.audit.controller;

import com.lastkey.backend.audit.dto.response.AuditLogResponse;
import com.lastkey.backend.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public Page<AuditLogResponse> getAllLogs(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("createdAt")
                                .descending()
                );

        return auditLogService.getAllLogs(
                pageable
        );
    }

    @GetMapping("/actor/{email}")
    public Page<AuditLogResponse> getActorLogs(

            @PathVariable
            String email,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return auditLogService.getLogsByActor(
                email,
                PageRequest.of(page, size)
        );
    }

    @GetMapping("/resource/{resource}")
    public Page<AuditLogResponse> getResourceLogs(

            @PathVariable
            String resource,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return auditLogService.getLogsByResource(
                resource,
                PageRequest.of(page, size)
        );
    }

    @GetMapping("/between")
    public Page<AuditLogResponse> getBetweenDates(

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime end,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return auditLogService.getLogsBetween(
                start,
                end,
                PageRequest.of(page, size)
        );
    }
}