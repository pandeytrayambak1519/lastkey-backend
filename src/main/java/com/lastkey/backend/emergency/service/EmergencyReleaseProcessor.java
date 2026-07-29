package com.lastkey.backend.emergency.service;

import java.util.UUID;

public interface EmergencyReleaseProcessor {

    /*
     * Processes one approved emergency request
     * inside an independent transaction.
     */
    void processEmergencyRelease(
            UUID emergencyRequestId
    );
}