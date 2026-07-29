package com.lastkey.backend.email.service;

import com.lastkey.backend.email.dto.EmailRequest;

public interface EmailService {

    void sendEmail(
            EmailRequest request
    );

    void sendSimpleEmail(
            String recipient,
            String subject,
            String message
    );

    void sendHtmlEmail(
            String recipient,
            String subject,
            String htmlContent
    );

    void sendWelcomeEmail(
            String recipient,
            String userName
    );

    void sendOtpEmail(
            String recipient,
            String userName,
            String otp,
            int expiryMinutes
    );

    void sendPasswordChangedEmail(
            String recipient,
            String userName
    );

    void sendDocumentExpiryEmail(
            String recipient,
            String userName,
            String documentTitle,
            long daysRemaining
    );

    void sendEmergencyActivatedEmail(
            String recipient,
            String nomineeName,
            String ownerName
    );

    void sendNomineeAccessGrantedEmail(
            String recipient,
            String nomineeName,
            String ownerName
    );
}