package com.lastkey.backend.nominee.service;

import com.lastkey.backend.nominee.dto.request.NomineeCreateRequest;
import com.lastkey.backend.nominee.dto.request.NomineeDocumentAccessRequest;
import com.lastkey.backend.nominee.dto.request.NomineeUpdateRequest;
import com.lastkey.backend.nominee.dto.response.NomineeDocumentResponse;
import com.lastkey.backend.nominee.dto.response.NomineeResponse;
import com.lastkey.backend.nominee.enums.NomineeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NomineeService {

    NomineeResponse createNominee(
            NomineeCreateRequest request,
            String userEmail
    );

    NomineeResponse getNomineeById(
            UUID nomineeId,
            String userEmail
    );

    Page<NomineeResponse> getAllNominees(
            String userEmail,
            NomineeStatus status,
            Pageable pageable
    );

    NomineeResponse updateNominee(
            UUID nomineeId,
            NomineeUpdateRequest request,
            String userEmail
    );

    void deleteNominee(
            UUID nomineeId,
            String userEmail
    );

    NomineeResponse setPrimaryNominee(
            UUID nomineeId,
            String userEmail
    );

    NomineeResponse resendVerificationOtp(
            UUID nomineeId,
            String userEmail
    );

    NomineeResponse verifyNominee(
            UUID nomineeId,
            String otp,
            String userEmail
    );

    NomineeDocumentResponse assignDocument(
            UUID nomineeId,
            UUID documentId,
            NomineeDocumentAccessRequest request,
            String userEmail
    );

    NomineeDocumentResponse updateDocumentAccess(
            UUID nomineeId,
            UUID documentId,
            NomineeDocumentAccessRequest request,
            String userEmail
    );

    void removeDocumentAccess(
            UUID nomineeId,
            UUID documentId,
            String userEmail
    );

    List<NomineeDocumentResponse> getAssignedDocuments(
            UUID nomineeId,
            String userEmail
    );
}