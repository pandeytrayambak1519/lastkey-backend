package com.lastkey.backend.emergency.service;

import java.util.UUID;

public interface EmergencyOtpService {

    /**
     * Generates and emails an OTP to the emergency-request owner.
     */
    void sendOtp(
            UUID emergencyRequestId,
            String currentUserEmail
    );

    /**
     * Verifies the OTP submitted by the emergency-request owner.
     */
    void verifyOtp(
            UUID emergencyRequestId,
            String otp,
            String currentUserEmail
    );

    /**
     * Returns true when the emergency request has a valid,
     * successfully verified OTP.
     */
    boolean isOtpVerified(
            UUID emergencyRequestId
    );

    /**
     * Throws an exception when OTP verification has not completed.
     */
    void requireOtpVerification(
            UUID emergencyRequestId
    );

    /**
     * Invalidates every currently usable OTP for a request.
     */
    void invalidateOtps(
            UUID emergencyRequestId
    );
}