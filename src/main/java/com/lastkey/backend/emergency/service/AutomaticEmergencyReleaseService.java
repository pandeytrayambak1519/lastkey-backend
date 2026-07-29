package com.lastkey.backend.emergency.service;

import java.util.UUID;

public interface AutomaticEmergencyReleaseService {

    /*
     * Finds and processes all emergency requests whose
     * scheduled document-release time has arrived.
     */
    void processScheduledReleases();

    /*
     * Processes one approved emergency request.
     */
    void processSingleRequest(
            UUID emergencyRequestId
    );
}