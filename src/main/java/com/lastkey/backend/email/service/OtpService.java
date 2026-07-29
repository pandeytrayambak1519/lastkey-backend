package com.lastkey.backend.email.service;

import com.lastkey.backend.email.enums.OtpPurpose;

public interface OtpService {

    void generateAndSendOtp(
            String email,
            String userName,
            OtpPurpose purpose
    );

    boolean verifyOtp(
            String email,
            String otp,
            OtpPurpose purpose
    );

    boolean isOtpVerified(
            String email,
            OtpPurpose purpose
    );

    void invalidateOtps(
            String email,
            OtpPurpose purpose
    );
}