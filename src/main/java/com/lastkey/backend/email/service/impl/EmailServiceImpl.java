package com.lastkey.backend.email.service.impl;

import com.lastkey.backend.email.dto.EmailRequest;
import com.lastkey.backend.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("otpEmailService")
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.mail.from:pandeytrayambak1519@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:LastKey}")
    private String fromName;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendEmail(EmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Email request is required");
        }
        sendSimpleEmail(request.getRecipient(), request.getSubject(), request.getMessage());
    }

    @Override
    @Async
    public void sendSimpleEmail(String recipient, String subject, String message) {
        validateEmailContent(recipient, subject, message);

        // Fallback to Brevo REST API if API Key is present (Bypasses Render SMTP Port Blocking)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendViaBrevoApi(recipient, subject, message);
            return;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(recipient.trim());
        mailMessage.setSubject(subject.trim());
        mailMessage.setText(message.trim());

        try {
            mailSender.send(mailMessage);
            log.info("Email sent via SMTP to {}", recipient);
        } catch (MailException exception) {
            log.error("Failed to send email to {}", recipient, exception);
        }
    }

    @Override
    @Async
    public void sendHtmlEmail(String recipient, String subject, String htmlContent) {
        validateEmailContent(recipient, subject, htmlContent);

        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            sendViaBrevoApi(recipient, subject, htmlContent);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipient.trim());
            helper.setSubject(subject.trim());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("HTML Email sent via SMTP to {}", recipient);
        } catch (MessagingException | MailException exception) {
            log.error("Failed to send HTML email to {}", recipient, exception);
        }
    }

    private void sendViaBrevoApi(String recipient, String subject, String content) {
        try {
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey.trim());

            Map<String, Object> sender = new HashMap<>();
            sender.put("email", fromEmail);
            sender.put("name", fromName);

            Map<String, Object> to = new HashMap<>();
            to.put("email", recipient.trim());

            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", Collections.singletonList(to));
            body.put("subject", subject.trim());
            
            // Format HTML paragraphs if content is simple text
            String htmlPayload = content.contains("<html") || content.contains("<p>") 
                    ? content 
                    : "<div style='font-family: Arial, sans-serif; white-space: pre-line;'>" + content + "</div>";
            
            body.put("htmlContent", htmlPayload);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent successfully via Brevo HTTPS API to {}", recipient);
            } else {
                log.error("Failed Brevo API Email to {}. Status: {}, Response: {}", recipient, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending email via Brevo HTTPS API to {}: {}", recipient, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String recipient, String userName) {
        String normalizedName = normalizeUserName(userName);
        String subject = "Welcome to LastKey";
        String message = """
                Hello %s,

                Welcome to LastKey.

                Your account has been created successfully.

                You can now securely manage your important documents,
                nominees and emergency access settings.

                Regards,
                LastKey Team
                """.formatted(normalizedName);

        sendSimpleEmail(recipient, subject, message);
    }

    @Override
    @Async
    public void sendOtpEmail(String recipient, String userName, String otp, int expiryMinutes) {
        validateOtp(otp, expiryMinutes);

        String normalizedName = normalizeUserName(userName);
        String subject = "LastKey - Email Verification OTP";
        String message = """
                Hello %s,

                Your One-Time Password (OTP) for LastKey is:

                %s

                This OTP is valid for %d minutes.

                Never share this OTP with anyone.
                LastKey will never ask you to provide your OTP.

                If you did not request this OTP, please ignore this email.

                Regards,
                LastKey Security Team
                """.formatted(normalizedName, otp, expiryMinutes);

        sendSimpleEmail(recipient, subject, message);
    }

    @Override
    @Async
    public void sendPasswordChangedEmail(String recipient, String userName) {
        String normalizedName = normalizeUserName(userName);
        String subject = "LastKey Password Changed";
        String message = """
                Hello %s,

                Your LastKey account password was changed successfully.

                If you made this change, no further action is required.

                If you did not change your password, please secure your
                account immediately and contact LastKey support.

                Regards,
                LastKey Security Team
                """.formatted(normalizedName);

        sendSimpleEmail(recipient, subject, message);
    }

    @Override
    @Async
    public void sendDocumentExpiryEmail(String recipient, String userName, String documentTitle, long daysRemaining) {
        String normalizedName = normalizeUserName(userName);
        String normalizedDocumentTitle = normalizeText(documentTitle, "Document");
        String subject = "LastKey Document Expiry Reminder";
        String message = """
                Hello %s,

                Your document "%s" will expire in %d day(s).

                Please review or renew the document before its expiry date.

                Regards,
                LastKey Team
                """.formatted(normalizedName, normalizedDocumentTitle, daysRemaining);

        sendSimpleEmail(recipient, subject, message);
    }

    @Override
    @Async
    public void sendEmergencyActivatedEmail(String recipient, String nomineeName, String ownerName) {
        String normalizedNomineeName = normalizeUserName(nomineeName);
        String normalizedOwnerName = normalizeText(ownerName, "the account owner");
        String subject = "LastKey Emergency Access Activated";
        String message = """
                Hello %s,

                Emergency access has been activated for the LastKey account
                belonging to %s.

                Please sign in to LastKey and follow the available
                instructions carefully.

                Regards,
                LastKey Security Team
                """.formatted(normalizedNomineeName, normalizedOwnerName);

        sendSimpleEmail(recipient, subject, message);
    }

    @Override
    @Async
    public void sendNomineeAccessGrantedEmail(String recipient, String nomineeName, String ownerName) {
        String normalizedNomineeName = normalizeUserName(nomineeName);
        String normalizedOwnerName = normalizeText(ownerName, "the account owner");
        String subject = "LastKey Nominee Access Granted";
        String message = """
                Hello %s,

                You have been granted nominee access by %s.

                Sign in to LastKey to review the documents and permissions
                shared with you.

                Regards,
                LastKey Team
                """.formatted(normalizedNomineeName, normalizedOwnerName);

        sendSimpleEmail(recipient, subject, message);
    }

    private void validateEmailContent(String recipient, String subject, String content) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Email content is required");
        }
    }

    private void validateOtp(String otp, int expiryMinutes) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP must contain exactly 6 digits");
        }
        if (expiryMinutes <= 0) {
            throw new IllegalArgumentException("OTP expiry time must be greater than zero");
        }
    }

    private String normalizeUserName(String userName) {
        return normalizeText(userName, "User");
    }

    private String normalizeText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}