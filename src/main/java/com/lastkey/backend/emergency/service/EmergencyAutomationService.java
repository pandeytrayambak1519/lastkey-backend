package com.lastkey.backend.emergency.service;

import java.util.UUID;

public interface EmergencyAutomationService {

    
    boolean releaseDocumentsAutomatically(
            UUID emergencyRequestId
    );
}