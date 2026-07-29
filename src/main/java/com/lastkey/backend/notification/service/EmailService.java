package com.lastkey.backend.notification.service;

public interface EmailService {

    void sendEmailVerificationOtp(
            String recipientEmail,
            String recipientName,
            String otp
    );
}