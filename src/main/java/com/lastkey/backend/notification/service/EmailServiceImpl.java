package com.lastkey.backend.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service("notificationEmailService")
public class EmailServiceImpl implements EmailService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmailVerificationOtp(
            String recipientEmail,
            String recipientName,
            String otp
    ) {

        validateRequest(
                recipientEmail,
                otp
        );

        String normalizedName =
                normalizeName(recipientName);

        String subject =
                "LastKey - Email Verification OTP";

        String body = """
                Hello %s,

                Your LastKey email verification OTP is:

                %s

                This OTP is valid for %d minutes.

                For your security:
                - Never share this OTP with anyone.
                - LastKey will never ask you for this OTP.
                - If you did not request this OTP, ignore this email.

                Regards,
                LastKey Security Team
                """.formatted(
                normalizedName,
                otp,
                OTP_EXPIRY_MINUTES
        );

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(recipientEmail.trim());
        message.setSubject(subject);
        message.setText(body);

        try {

            mailSender.send(message);

        } catch (MailException exception) {

            throw new IllegalStateException(
                    "Failed to send email verification OTP",
                    exception
            );
        }
    }

    private void validateRequest(
            String recipientEmail,
            String otp
    ) {

        if (recipientEmail == null
                || recipientEmail.isBlank()) {

            throw new IllegalArgumentException(
                    "Recipient email is required"
            );
        }

        if (otp == null
                || !otp.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must contain exactly 6 digits"
            );
        }
    }

    private String normalizeName(
            String recipientName
    ) {

        if (recipientName == null
                || recipientName.isBlank()) {

            return "User";
        }

        return recipientName.trim();
    }
}