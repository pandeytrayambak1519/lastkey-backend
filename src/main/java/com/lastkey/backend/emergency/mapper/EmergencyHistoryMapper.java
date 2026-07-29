package com.lastkey.backend.emergency.mapper;

import com.lastkey.backend.emergency.dto.response.EmergencyHistoryResponse;
import com.lastkey.backend.emergency.entity.EmergencyRequestLog;
import org.springframework.stereotype.Component;

@Component
public class EmergencyHistoryMapper {

    public EmergencyHistoryResponse toResponse(
            EmergencyRequestLog log
    ) {

        if (log == null) {
            return null;
        }

        return EmergencyHistoryResponse.builder()
                .id(log.getId())
                .emergencyRequestId(
                        log.getEmergencyRequest() != null
                                ? log.getEmergencyRequest().getId()
                                : null
                )
                .action(log.getAction())
                .previousStatus(log.getPreviousStatus())
                .newStatus(log.getNewStatus())
                .performedBy(log.getPerformedBy())
                .performedByType(log.getPerformedByType())
                .message(log.getMessage())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}