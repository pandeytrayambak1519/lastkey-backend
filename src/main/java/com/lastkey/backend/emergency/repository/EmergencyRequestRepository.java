package com.lastkey.backend.emergency.repository;

import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyRequestRepository
        extends JpaRepository<EmergencyRequest, UUID> {

    boolean existsByOwnerAndNomineeAndStatusIn(
            User owner,
            Nominee nominee,
            List<EmergencyStatus> statuses
    );

    Page<EmergencyRequest> findByOwner(
            User owner,
            Pageable pageable
    );

    Optional<EmergencyRequest> findByIdAndOwner(
            UUID id,
            User owner
    );

    /*
     * Dashboard:
     * Counts active emergency requests of the user.
     */
    long countByOwnerAndActiveTrue(
            User owner
    );

    /*
     * Finds requests eligible for automatic document release.
     */
    List<EmergencyRequest>
    findByStatusAndActiveTrueAndScheduledReleaseAtLessThanEqual(
            EmergencyStatus status,
            LocalDateTime currentTime
    );
}