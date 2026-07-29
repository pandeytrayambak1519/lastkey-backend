package com.lastkey.backend.nomineeaccess.service;

import com.lastkey.backend.nomineeaccess.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface NomineeSecureAccessService {

    List<NomineeAccessibleDocumentResponse>
    getAccessibleDocuments(
            String nomineeEmail,
            String ipAddress,
            String userAgent
    );

    NomineeDocumentDetailsResponse
    getDocumentDetails(
            UUID documentId,
            String nomineeEmail,
            String ipAddress,
            String userAgent
    );

    NomineeFileAccessResponse viewDocument(
            UUID documentId,
            String nomineeEmail,
            String ipAddress,
            String userAgent
    );

    NomineeFileAccessResponse downloadDocument(
            UUID documentId,
            String nomineeEmail,
            String ipAddress,
            String userAgent
    );

    Page<NomineeAccessHistoryResponse>
    getAccessHistory(
            String nomineeEmail,
            int page,
            int size
    );
}