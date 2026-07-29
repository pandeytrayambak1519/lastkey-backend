package com.lastkey.backend.nomineeaccess.controller;

import com.lastkey.backend.nomineeaccess.dto.*;
import com.lastkey.backend.nomineeaccess.service.NomineeSecureAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nominee-access")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NomineeSecureAccessController {

    private final NomineeSecureAccessService
            nomineeSecureAccessService;

    @GetMapping("/documents")
    public ResponseEntity<
            List<NomineeAccessibleDocumentResponse>
            > getAccessibleDocuments(
            Authentication authentication,
            HttpServletRequest request
    ) {

        return ResponseEntity.ok(
                nomineeSecureAccessService
                        .getAccessibleDocuments(
                                authentication.getName(),
                                getClientIp(request),
                                request.getHeader(
                                        "User-Agent"
                                )
                        )
        );
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<
            NomineeDocumentDetailsResponse
            > getDocumentDetails(
            @PathVariable UUID documentId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        return ResponseEntity.ok(
                nomineeSecureAccessService
                        .getDocumentDetails(
                                documentId,
                                authentication.getName(),
                                getClientIp(request),
                                request.getHeader(
                                        "User-Agent"
                                )
                        )
        );
    }

    @GetMapping("/documents/{documentId}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable UUID documentId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        NomineeFileAccessResponse response =
                nomineeSecureAccessService
                        .viewDocument(
                                documentId,
                                authentication.getName(),
                                getClientIp(request),
                                request.getHeader(
                                        "User-Agent"
                                )
                        );

        MediaType mediaType =
                resolveMediaType(
                        response.getMimeType()
                );

        ContentDisposition disposition =
                ContentDisposition.inline()
                        .filename(
                                response.getFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(
                        resolveContentLength(response)
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate"
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(response.getResource());
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        NomineeFileAccessResponse response =
                nomineeSecureAccessService
                        .downloadDocument(
                                documentId,
                                authentication.getName(),
                                getClientIp(request),
                                request.getHeader(
                                        "User-Agent"
                                )
                        );

        MediaType mediaType =
                resolveMediaType(
                        response.getMimeType()
                );

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                response.getFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(
                        resolveContentLength(response)
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate"
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(response.getResource());
    }

    @GetMapping("/access-history")
    public ResponseEntity<
            Page<NomineeAccessHistoryResponse>
            > getAccessHistory(
            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            Authentication authentication
    ) {

        return ResponseEntity.ok(
                nomineeSecureAccessService
                        .getAccessHistory(
                                authentication.getName(),
                                page,
                                size
                        )
        );
    }

    private String getClientIp(
            HttpServletRequest request
    ) {

        String forwardedFor =
                request.getHeader(
                        "X-Forwarded-For"
                );

        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        String realIp =
                request.getHeader(
                        "X-Real-IP"
                );

        if (realIp != null
                && !realIp.isBlank()) {

            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private MediaType resolveMediaType(
            String mimeType
    ) {

        if (mimeType == null
                || mimeType.isBlank()) {

            return MediaType
                    .APPLICATION_OCTET_STREAM;
        }

        try {

            return MediaType.parseMediaType(
                    mimeType
            );

        } catch (InvalidMediaTypeException exception) {

            return MediaType
                    .APPLICATION_OCTET_STREAM;
        }
    }

    private long resolveContentLength(
            NomineeFileAccessResponse response
    ) {

        if (response.getFileSize() != null
                && response.getFileSize() >= 0) {

            return response.getFileSize();
        }

        try {

            return response.getResource()
                    .contentLength();

        } catch (Exception exception) {

            return 0;
        }
    }
}