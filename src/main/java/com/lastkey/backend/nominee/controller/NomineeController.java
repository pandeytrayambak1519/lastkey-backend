package com.lastkey.backend.nominee.controller;

import com.lastkey.backend.nominee.dto.request.NomineeCreateRequest;
import com.lastkey.backend.nominee.dto.request.NomineeDocumentAccessRequest;
import com.lastkey.backend.nominee.dto.request.NomineeUpdateRequest;
import com.lastkey.backend.nominee.dto.request.NomineeVerificationRequest;
import com.lastkey.backend.nominee.dto.response.NomineeDocumentResponse;
import com.lastkey.backend.nominee.dto.response.NomineeResponse;
import com.lastkey.backend.nominee.enums.NomineeStatus;
import com.lastkey.backend.nominee.service.NomineeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nominees")
@RequiredArgsConstructor
public class NomineeController {

    private final NomineeService nomineeService;

    @PostMapping
    public ResponseEntity<NomineeResponse> createNominee(
            @Valid @RequestBody NomineeCreateRequest request,
            Authentication authentication
    ) {

        NomineeResponse response =
                nomineeService.createNominee(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{nomineeId}")
    public ResponseEntity<NomineeResponse> getNomineeById(
            @PathVariable UUID nomineeId,
            Authentication authentication
    ) {

        NomineeResponse response =
                nomineeService.getNomineeById(
                        nomineeId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<NomineeResponse>> getAllNominees(
            @RequestParam(required = false)
            NomineeStatus status,
            Pageable pageable,
            Authentication authentication
    ) {

        Page<NomineeResponse> response =
                nomineeService.getAllNominees(
                        authentication.getName(),
                        status,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{nomineeId}")
    public ResponseEntity<NomineeResponse> updateNominee(
            @PathVariable UUID nomineeId,
            @Valid @RequestBody NomineeUpdateRequest request,
            Authentication authentication
    ) {

        NomineeResponse response =
                nomineeService.updateNominee(
                        nomineeId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{nomineeId}")
    public ResponseEntity<Void> deleteNominee(
            @PathVariable UUID nomineeId,
            Authentication authentication
    ) {

        nomineeService.deleteNominee(
                nomineeId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{nomineeId}/primary")
    public ResponseEntity<NomineeResponse> setPrimaryNominee(
            @PathVariable UUID nomineeId,
            Authentication authentication
    ) {

        NomineeResponse response =
                nomineeService.setPrimaryNominee(
                        nomineeId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{nomineeId}/resend-verification-otp")
    public ResponseEntity<NomineeResponse> resendVerificationOtp(
            @PathVariable UUID nomineeId,
            Authentication authentication
    ) {

        NomineeResponse response =
                nomineeService.resendVerificationOtp(
                        nomineeId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{nomineeId}/verify")
    public ResponseEntity<NomineeResponse> verifyNominee(
            @PathVariable UUID nomineeId,
            @Valid @RequestBody
            NomineeVerificationRequest request,
            Authentication authentication
    ) {

        NomineeResponse response =
                nomineeService.verifyNominee(
                        nomineeId,
                        request.getOtp(),
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{nomineeId}/documents/{documentId}")
    public ResponseEntity<NomineeDocumentResponse> assignDocument(
            @PathVariable UUID nomineeId,
            @PathVariable UUID documentId,
            @Valid @RequestBody
            NomineeDocumentAccessRequest request,
            Authentication authentication
    ) {

        NomineeDocumentResponse response =
                nomineeService.assignDocument(
                        nomineeId,
                        documentId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{nomineeId}/documents/{documentId}")
    public ResponseEntity<NomineeDocumentResponse>
    updateDocumentAccess(
            @PathVariable UUID nomineeId,
            @PathVariable UUID documentId,
            @Valid @RequestBody
            NomineeDocumentAccessRequest request,
            Authentication authentication
    ) {

        NomineeDocumentResponse response =
                nomineeService.updateDocumentAccess(
                        nomineeId,
                        documentId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{nomineeId}/documents/{documentId}")
    public ResponseEntity<Void> removeDocumentAccess(
            @PathVariable UUID nomineeId,
            @PathVariable UUID documentId,
            Authentication authentication
    ) {

        nomineeService.removeDocumentAccess(
                nomineeId,
                documentId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{nomineeId}/documents")
    public ResponseEntity<List<NomineeDocumentResponse>>
    getAssignedDocuments(
            @PathVariable UUID nomineeId,
            Authentication authentication
    ) {

        List<NomineeDocumentResponse> response =
                nomineeService.getAssignedDocuments(
                        nomineeId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }
}