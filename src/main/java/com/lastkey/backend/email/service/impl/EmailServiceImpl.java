package com.lastkey.backend.email.service.impl;

import com.lastkey.backend.email.dto.EmailRequest;
import com.lastkey.backend.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service("otpEmailService")
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailServiceImpl(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(
            EmailRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Email request is required"
            );
        }

        sendSimpleEmail(
                request.getRecipient(),
                request.getSubject(),
                request.getMessage()
        );
    }

    @Override
    public void sendSimpleEmail(
            String recipient,
            String subject,
            String message
    ) {

        validateEmailContent(
                recipient,
                subject,
                message
        );

        SimpleMailMessage mailMessage =
                new SimpleMailMessage();

        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(recipient.trim());
        mailMessage.setSubject(subject.trim());
        mailMessage.setText(message.trim());

        try {

            mailSender.send(mailMessage);

        } catch (MailException exception) {

            throw new IllegalStateException(
                    "Failed to send email to "
                            + recipient,
                    exception
            );
        }
    }

    @Override
    public void sendHtmlEmail(
            String recipient,
            String subject,
            String htmlContent
    ) {

        validateEmailContent(
                recipient,
                subject,
                htmlContent
        );

        try {

            MimeMessage mimeMessage =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mimeMessage,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(fromEmail);
            helper.setTo(recipient.trim());
            helper.setSubject(subject.trim());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException | MailException exception) {

            throw new IllegalStateException(
                    "Failed to send HTML email to "
                            + recipient,
                    exception
            );
        }
    }

    @Override
    public void sendWelcomeEmail(
            String recipient,
            String userName
    ) {

        String normalizedName =
                normalizeUserName(userName);

        String subject =
                "Welcome to LastKey";

        String message = """
                Hello %s,

                Welcome to LastKey.

                Your account has been created successfully.

                You can now securely manage your important documents,
                nominees and emergency access settings.

                Regards,
                LastKey Team
                """.formatted(normalizedName);

        sendSimpleEmail(
                recipient,
                subject,
                message
        );
    }

    @Override
    public void sendOtpEmail(
            String recipient,
            String userName,
            String otp,
            int expiryMinutes
    ) {

        validateOtp(
                otp,
                expiryMinutes
        );

        String normalizedName =
                normalizeUserName(userName);

        String subject =
                "LastKey - Email Verification OTP";

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
                """.formatted(
                normalizedName,
                otp,
                expiryMinutes
        );

        sendSimpleEmail(
                recipient,
                subject,
                message
        );
    }

    @Override
    public void sendPasswordChangedEmail(
            String recipient,
            String userName
    ) {

        String normalizedName =
                normalizeUserName(userName);

        String subject =
                "LastKey Password Changed";

        String message = """
                Hello %s,

                Your LastKey account password was changed successfully.

                If you made this change, no further action is required.

                If you did not change your password, please secure your
                account immediately and contact LastKey support.

                Regards,
                LastKey Security Team
                """.formatted(normalizedName);

        sendSimpleEmail(
                recipient,
                subject,
                message
        );
    }

    @Override
    public void sendDocumentExpiryEmail(
            String recipient,
            String userName,
            String documentTitle,
            long daysRemaining
    ) {

        String normalizedName =
                normalizeUserName(userName);

        String normalizedDocumentTitle =
                normalizeText(
                        documentTitle,
                        "Document"
                );

        String subject =
                "LastKey Document Expiry Reminder";

        String message = """
                Hello %s,

                Your document "%s" will expire in %d day(s).

                Please review or renew the document before its expiry date.

                Regards,
                LastKey Team
                """.formatted(
                normalizedName,
                normalizedDocumentTitle,
                daysRemaining
        );

        sendSimpleEmail(
                recipient,
                subject,
                message
        );
    }

    @Override
    public void sendEmergencyActivatedEmail(
            String recipient,
            String nomineeName,
            String ownerName
    ) {

        String normalizedNomineeName =
                normalizeUserName(nomineeName);

        String normalizedOwnerName =
                normalizeText(
                        ownerName,
                        "the account owner"
                );

        String subject =
                "LastKey Emergency Access Activated";

        String message = """
                Hello %s,

                Emergency access has been activated for the LastKey account
                belonging to %s.

                Please sign in to LastKey and follow the available
                instructions carefully.

                Regards,
                LastKey Security Team
                """.formatted(
                normalizedNomineeName,
                normalizedOwnerName
        );

        sendSimpleEmail(
                recipient,
                subject,
                message
        );
    }

    @Override
    public void sendNomineeAccessGrantedEmail(
            String recipient,
            String nomineeName,
            String ownerName
    ) {

        String normalizedNomineeName =
                normalizeUserName(nomineeName);

        String normalizedOwnerName =
                normalizeText(
                        ownerName,
                        "the account owner"
                );

        String subject =
                "LastKey Nominee Access Granted";

        String message = """
                Hello %s,

                You have been granted nominee access by %s.

                Sign in to LastKey to review the documents and permissions
                shared with you.

                Regards,
                LastKey Team
                """.formatted(
                normalizedNomineeName,
                normalizedOwnerName
        );

        sendSimpleEmail(
                recipient,
                subject,
                message
        );
    }

    private void validateEmailContent(
            String recipient,
            String subject,
            String content
    ) {

        if (recipient == null
                || recipient.isBlank()) {

            throw new IllegalArgumentException(
                    "Recipient email is required"
            );
        }

        if (subject == null
                || subject.isBlank()) {

            throw new IllegalArgumentException(
                    "Email subject is required"
            );
        }

        if (content == null
                || content.isBlank()) {

            throw new IllegalArgumentException(
                    "Email content is required"
            );
        }
    }

    private void validateOtp(
            String otp,
            int expiryMinutes
    ) {

        if (otp == null
                || !otp.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must contain exactly 6 digits"
            );
        }

        if (expiryMinutes <= 0) {

            throw new IllegalArgumentException(
                    "OTP expiry time must be greater than zero"
            );
        }
    }

    private String normalizeUserName(
            String userName
    ) {

        return normalizeText(
                userName,
                "User"
        );
    }

    private String normalizeText(
            String value,
            String defaultValue
    ) {

        if (value == null
                || value.isBlank()) {

            return defaultValue;
        }

        return value.trim();
    }
}