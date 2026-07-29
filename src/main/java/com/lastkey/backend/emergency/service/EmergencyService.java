package com.lastkey.backend.emergency.service;

import com.lastkey.backend.emergency.dto.request.CreateEmergencyRequest;
import com.lastkey.backend.emergency.dto.request.EmergencyActionRequest;
import com.lastkey.backend.emergency.dto.request.UpdateEmergencyRequest;
import com.lastkey.backend.emergency.dto.response.EmergencyHistoryResponse;
import com.lastkey.backend.emergency.dto.response.EmergencyReleaseResponse;
import com.lastkey.backend.emergency.dto.response.EmergencyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EmergencyService {

    EmergencyResponse createEmergencyRequest(
            CreateEmergencyRequest request,
            String currentUserEmail
    );

    EmergencyResponse getEmergencyRequestById(
            UUID emergencyRequestId,
            String currentUserEmail
    );

    Page<EmergencyResponse> getOwnerEmergencyRequests(
            String currentUserEmail,
            Pageable pageable
    );

    EmergencyResponse updateEmergencyRequest(
            UUID emergencyRequestId,
            UpdateEmergencyRequest request,
            String currentUserEmail
    );

    EmergencyResponse cancelEmergencyRequest(
            UUID emergencyRequestId,
            EmergencyActionRequest request,
            String currentUserEmail
    );

    EmergencyResponse approveEmergencyRequest(
            UUID emergencyRequestId,
            EmergencyActionRequest request,
            String performedByEmail
    );

    EmergencyResponse rejectEmergencyRequest(
            UUID emergencyRequestId,
            EmergencyActionRequest request,
            String performedByEmail
    );

    List<EmergencyHistoryResponse> getEmergencyRequestHistory(
            UUID emergencyRequestId,
            String currentUserEmail
    );

    List<EmergencyReleaseResponse> releaseDocuments(
            UUID emergencyRequestId,
            String performedByEmail
    );

    List<EmergencyReleaseResponse> getReleasedDocuments(
            UUID emergencyRequestId,
            String currentUserEmail
    );

    void revokeReleasedDocument(
            UUID releaseHistoryId,
            String reason,
            String performedByEmail
    );
}