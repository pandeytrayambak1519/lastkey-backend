package com.lastkey.backend.emergency.controller;

import com.lastkey.backend.emergency.dto.request.CreateEmergencyRequest;
import com.lastkey.backend.emergency.dto.request.EmergencyActionRequest;
import com.lastkey.backend.emergency.dto.request.UpdateEmergencyRequest;
import com.lastkey.backend.emergency.dto.response.EmergencyHistoryResponse;
import com.lastkey.backend.emergency.dto.response.EmergencyReleaseResponse;
import com.lastkey.backend.emergency.dto.response.EmergencyResponse;
import com.lastkey.backend.emergency.service.EmergencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergencies")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyService emergencyService;

    @PostMapping
    public ResponseEntity<EmergencyResponse> createEmergencyRequest(
            @Valid
            @RequestBody
            CreateEmergencyRequest request,

            Authentication authentication
    ) {

        EmergencyResponse response =
                emergencyService.createEmergencyRequest(
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<EmergencyResponse>>
    getOwnerEmergencyRequests(

            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt"
            )
            Pageable pageable,

            Authentication authentication
    ) {

        Page<EmergencyResponse> response =
                emergencyService.getOwnerEmergencyRequests(
                        getAuthenticatedEmail(authentication),
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{emergencyRequestId}")
    public ResponseEntity<EmergencyResponse>
    getEmergencyRequestById(

            @PathVariable
            UUID emergencyRequestId,

            Authentication authentication
    ) {

        EmergencyResponse response =
                emergencyService.getEmergencyRequestById(
                        emergencyRequestId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{emergencyRequestId}")
    public ResponseEntity<EmergencyResponse>
    updateEmergencyRequest(

            @PathVariable
            UUID emergencyRequestId,

            @Valid
            @RequestBody
            UpdateEmergencyRequest request,

            Authentication authentication
    ) {

        EmergencyResponse response =
                emergencyService.updateEmergencyRequest(
                        emergencyRequestId,
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{emergencyRequestId}/cancel")
    public ResponseEntity<EmergencyResponse>
    cancelEmergencyRequest(

            @PathVariable
            UUID emergencyRequestId,

            @Valid
            @RequestBody(required = false)
            EmergencyActionRequest request,

            Authentication authentication
    ) {

        EmergencyResponse response =
                emergencyService.cancelEmergencyRequest(
                        emergencyRequestId,
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{emergencyRequestId}/approve")
    public ResponseEntity<EmergencyResponse>
    approveEmergencyRequest(

            @PathVariable
            UUID emergencyRequestId,

            @Valid
            @RequestBody(required = false)
            EmergencyActionRequest request,

            Authentication authentication
    ) {

        EmergencyResponse response =
                emergencyService.approveEmergencyRequest(
                        emergencyRequestId,
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{emergencyRequestId}/reject")
    public ResponseEntity<EmergencyResponse>
    rejectEmergencyRequest(

            @PathVariable
            UUID emergencyRequestId,

            @Valid
            @RequestBody
            EmergencyActionRequest request,

            Authentication authentication
    ) {

        EmergencyResponse response =
                emergencyService.rejectEmergencyRequest(
                        emergencyRequestId,
                        request,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{emergencyRequestId}/history")
    public ResponseEntity<List<EmergencyHistoryResponse>>
    getEmergencyRequestHistory(

            @PathVariable
            UUID emergencyRequestId,

            Authentication authentication
    ) {

        List<EmergencyHistoryResponse> response =
                emergencyService.getEmergencyRequestHistory(
                        emergencyRequestId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{emergencyRequestId}/release")
    public ResponseEntity<List<EmergencyReleaseResponse>>
    releaseDocuments(

            @PathVariable
            UUID emergencyRequestId,

            Authentication authentication
    ) {

        List<EmergencyReleaseResponse> response =
                emergencyService.releaseDocuments(
                        emergencyRequestId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{emergencyRequestId}/released-documents")
    public ResponseEntity<List<EmergencyReleaseResponse>>
    getReleasedDocuments(

            @PathVariable
            UUID emergencyRequestId,

            Authentication authentication
    ) {

        List<EmergencyReleaseResponse> response =
                emergencyService.getReleasedDocuments(
                        emergencyRequestId,
                        getAuthenticatedEmail(authentication)
                );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/releases/{releaseHistoryId}/revoke")
    public ResponseEntity<Void>
    revokeReleasedDocument(

            @PathVariable
            UUID releaseHistoryId,

            @Valid
            @RequestBody
            EmergencyActionRequest request,

            Authentication authentication
    ) {

        emergencyService.revokeReleasedDocument(
                releaseHistoryId,
                request.getMessage(),
                getAuthenticatedEmail(authentication)
        );

        return ResponseEntity.noContent().build();
    }


    private String getAuthenticatedEmail(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "Authenticated user information is unavailable"
            );
        }

        return authentication.getName();
    }
}