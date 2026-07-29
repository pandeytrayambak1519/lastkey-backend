package com.lastkey.backend.emergency.repository;

import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.entity.EmergencyRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyRequestLogRepository
        extends JpaRepository<EmergencyRequestLog, UUID> {

    List<EmergencyRequestLog> findByEmergencyRequestOrderByCreatedAtAsc(
            EmergencyRequest emergencyRequest
    );
}