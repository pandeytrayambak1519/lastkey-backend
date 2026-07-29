package com.lastkey.backend.auth.service.impl;

import com.lastkey.backend.auth.dto.request.ResendOtpRequest;
import com.lastkey.backend.auth.dto.request.VerifyEmailRequest;
import com.lastkey.backend.auth.dto.response.EmailVerificationResponse;
import com.lastkey.backend.auth.service.EmailVerificationService;
import com.lastkey.backend.email.enums.OtpPurpose;
import com.lastkey.backend.email.service.OtpService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class EmailVerificationServiceImpl
        implements EmailVerificationService {

    private final UserRepository userRepository;
    private final OtpService otpService;

    public EmailVerificationServiceImpl(
            UserRepository userRepository,
            OtpService otpService
    ) {
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    // =========================================================
    // Generate and send email-verification OTP
    // =========================================================

    @Override
    @Transactional
    public void createAndSendOtp(
            User user
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User information is required"
            );
        }

        if (user.getEmail() == null
                || user.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "User email is required"
            );
        }

        if (Boolean.TRUE.equals(
                user.getEmailVerified()
        )) {

            throw new IllegalStateException(
                    "Email is already verified"
            );
        }

        otpService.generateAndSendOtp(
                normalizeEmail(user.getEmail()),
                buildFullName(user),
                OtpPurpose.EMAIL_VERIFICATION
        );
    }

    // =========================================================
    // Verify email using OTP
    // =========================================================

    @Override
    @Transactional
    public EmailVerificationResponse verifyEmail(
            VerifyEmailRequest request
    ) {

        validateVerifyEmailRequest(request);

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "User not found with this email"
                                )
                        );

        if (Boolean.TRUE.equals(
                user.getEmailVerified()
        )) {

            return new EmailVerificationResponse(
                    "Email is already verified",
                    user.getEmail(),
                    true
            );
        }

        boolean verified =
                otpService.verifyOtp(
                        normalizedEmail,
                        request.getOtp().trim(),
                        OtpPurpose.EMAIL_VERIFICATION
                );

        if (!verified) {
            throw new IllegalArgumentException(
                    "Invalid or expired verification OTP"
            );
        }

        user.setEmailVerified(true);

        userRepository.save(user);

        /*
         * Remove any remaining active email-verification OTPs.
         */
        otpService.invalidateOtps(
                normalizedEmail,
                OtpPurpose.EMAIL_VERIFICATION
        );

        return new EmailVerificationResponse(
                "Email verified successfully",
                user.getEmail(),
                true
        );
    }

    // =========================================================
    // Resend email-verification OTP
    // =========================================================

    @Override
    @Transactional
    public EmailVerificationResponse resendOtp(
            ResendOtpRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Resend OTP request is required"
            );
        }

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "User not found with this email"
                                )
                        );

        if (Boolean.TRUE.equals(
                user.getEmailVerified()
        )) {

            return new EmailVerificationResponse(
                    "Email is already verified",
                    user.getEmail(),
                    true
            );
        }

        /*
         * Invalidate previous active OTP before issuing another.
         */
        otpService.invalidateOtps(
                normalizedEmail,
                OtpPurpose.EMAIL_VERIFICATION
        );

        otpService.generateAndSendOtp(
                normalizedEmail,
                buildFullName(user),
                OtpPurpose.EMAIL_VERIFICATION
        );

        return new EmailVerificationResponse(
                "A new verification OTP has been sent",
                user.getEmail(),
                false
        );
    }

    // =========================================================
    // Validate verification request
    // =========================================================

    private void validateVerifyEmailRequest(
            VerifyEmailRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Email-verification request is required"
            );
        }

        normalizeEmail(request.getEmail());

        if (request.getOtp() == null
                || request.getOtp().isBlank()) {

            throw new IllegalArgumentException(
                    "OTP is required"
            );
        }

        if (!request.getOtp()
                .trim()
                .matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must contain exactly 6 digits"
            );
        }
    }

    // =========================================================
    // Normalize email
    // =========================================================

    private String normalizeEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        String normalizedEmail =
                email.trim()
                        .toLowerCase(Locale.ROOT);

        if (!normalizedEmail.contains("@")
                || normalizedEmail.startsWith("@")
                || normalizedEmail.endsWith("@")) {

            throw new IllegalArgumentException(
                    "Invalid email address"
            );
        }

        return normalizedEmail;
    }

    // =========================================================
    // Build recipient name
    // =========================================================

    private String buildFullName(
            User user
    ) {

        String firstName =
                user.getFirstName() == null
                        ? ""
                        : user.getFirstName().trim();

        String lastName =
                user.getLastName() == null
                        ? ""
                        : user.getLastName().trim();

        String fullName =
                (firstName + " " + lastName).trim();

        return fullName.isBlank()
                ? "User"
                : fullName;
    }
}