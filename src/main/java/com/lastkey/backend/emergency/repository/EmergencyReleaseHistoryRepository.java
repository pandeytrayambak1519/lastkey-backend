package com.lastkey.backend.emergency.repository;

import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.emergency.entity.EmergencyReleaseHistory;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.nominee.entity.Nominee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyReleaseHistoryRepository
        extends JpaRepository<EmergencyReleaseHistory, UUID> {

    List<EmergencyReleaseHistory>
    findByEmergencyRequestOrderByReleasedAtDesc(
            EmergencyRequest emergencyRequest
    );

    List<EmergencyReleaseHistory>
    findByEmergencyRequest(
            EmergencyRequest emergencyRequest
    );

    List<EmergencyReleaseHistory>
    findByNominee(
            Nominee nominee
    );

    List<EmergencyReleaseHistory>
    findByNomineeOrderByReleasedAtDesc(
            Nominee nominee
    );

    Optional<EmergencyReleaseHistory>
    findByIdAndNominee(
            UUID releaseHistoryId,
            Nominee nominee
    );

    Optional<EmergencyReleaseHistory>
    findByNomineeAndDocument(
            Nominee nominee,
            Document document
    );

    Optional<EmergencyReleaseHistory>
    findFirstByNomineeAndDocumentIdOrderByReleasedAtDesc(
            Nominee nominee,
            UUID documentId
    );

    List<EmergencyReleaseHistory>
    findByDocument(
            Document document
    );

    boolean existsByEmergencyRequestAndDocumentAndNominee(
            EmergencyRequest emergencyRequest,
            Document document,
            Nominee nominee
    );

    boolean existsByEmergencyRequestAndDocument(
            EmergencyRequest emergencyRequest,
            Document document
    );

    List<EmergencyReleaseHistory>
    findByAccessRevokedFalseAndAccessExpiresAtLessThanEqual(
            LocalDateTime currentTime
    );
}