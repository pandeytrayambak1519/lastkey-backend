package com.lastkey.backend.email.service.impl;

import com.lastkey.backend.email.entity.OtpVerification;
import com.lastkey.backend.email.enums.OtpPurpose;
import com.lastkey.backend.email.repository.OtpVerificationRepository;
import com.lastkey.backend.email.service.EmailService;
import com.lastkey.backend.email.service.OtpService;
import com.lastkey.backend.email.util.OtpGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class OtpServiceImpl implements OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    private final OtpVerificationRepository otpRepository;
    private final OtpGenerator otpGenerator;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public OtpServiceImpl(
            OtpVerificationRepository otpRepository,
            OtpGenerator otpGenerator,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.otpRepository = otpRepository;
        this.otpGenerator = otpGenerator;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void generateAndSendOtp(
            String email,
            String userName,
            OtpPurpose purpose
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        String normalizedUserName =
                normalizeUserName(userName);

        validatePurpose(purpose);

        validateResendCooldown(
                normalizedEmail,
                purpose
        );

        /*
         * Remove the previous record for the same email and purpose.
         *
         * Merely setting used=true is not enough when the database
         * has a unique constraint on email and purpose.
         */
        otpRepository.deleteByEmailAndPurpose(
                normalizedEmail,
                purpose
        );

        /*
         * Ensure the delete query reaches the database before the
         * next INSERT operation.
         */
        otpRepository.flush();

        String plainOtp =
                otpGenerator.generateOtp();

        validateGeneratedOtp(plainOtp);

        String encodedOtp =
                passwordEncoder.encode(plainOtp);

        LocalDateTime now =
                LocalDateTime.now();

        OtpVerification otpVerification =
                OtpVerification.builder()
                        .email(normalizedEmail)
                        .otpHash(encodedOtp)
                        .purpose(purpose)
                        .expiresAt(
                                now.plusMinutes(
                                        OTP_EXPIRY_MINUTES
                                )
                        )
                        .verified(false)
                        .used(false)
                        .attempts(0)
                        .maxAttempts(MAX_OTP_ATTEMPTS)
                        .build();

        otpRepository.saveAndFlush(
                otpVerification
        );

        try {

            emailService.sendOtpEmail(
                    normalizedEmail,
                    normalizedUserName,
                    plainOtp,
                    OTP_EXPIRY_MINUTES
            );

        } catch (RuntimeException exception) {

            throw new IllegalStateException(
                    "OTP could not be sent to the email address",
                    exception
            );
        }
    }

    @Override
    @Transactional
    public boolean verifyOtp(
            String email,
            String otp,
            OtpPurpose purpose
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        String normalizedOtp =
                normalizeOtp(otp);

        validatePurpose(purpose);

        OtpVerification otpVerification =
                otpRepository
                        .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                                normalizedEmail,
                                purpose
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "No active OTP was found. Please request a new OTP"
                                )
                        );

        if (otpVerification.isExpired()) {

            otpVerification.invalidate();

            otpRepository.save(
                    otpVerification
            );

            return false;
        }

        if (otpVerification
                .hasReachedMaximumAttempts()) {

            otpVerification.invalidate();

            otpRepository.save(
                    otpVerification
            );

            return false;
        }

        boolean matches =
                passwordEncoder.matches(
                        normalizedOtp,
                        otpVerification.getOtpHash()
                );

        if (!matches) {

            otpVerification.increaseAttempts();

            if (otpVerification
                    .hasReachedMaximumAttempts()) {

                otpVerification.invalidate();
            }

            otpRepository.save(
                    otpVerification
            );

            return false;
        }

        otpVerification.markVerified();

        otpRepository.save(
                otpVerification
        );

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOtpVerified(
            String email,
            OtpPurpose purpose
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        validatePurpose(purpose);

        return otpRepository
                .existsByEmailAndPurposeAndVerifiedTrue(
                        normalizedEmail,
                        purpose
                );
    }

    @Override
    @Transactional
    public void invalidateOtps(
            String email,
            OtpPurpose purpose
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        validatePurpose(purpose);

        otpRepository.invalidateExistingOtps(
                normalizedEmail,
                purpose,
                LocalDateTime.now()
        );
    }

    private void validateResendCooldown(
            String email,
            OtpPurpose purpose
    ) {

        otpRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(
                        email,
                        purpose
                )
                .ifPresent(
                        previousOtp -> {

                            LocalDateTime createdAt =
                                    previousOtp.getCreatedAt();

                            if (createdAt == null) {
                                return;
                            }

                            LocalDateTime cooldownEnd =
                                    createdAt.plusSeconds(
                                            OTP_RESEND_COOLDOWN_SECONDS
                                    );

                            LocalDateTime now =
                                    LocalDateTime.now();

                            if (now.isBefore(cooldownEnd)) {

                                long remainingSeconds =
                                        ChronoUnit.SECONDS.between(
                                                now,
                                                cooldownEnd
                                        );

                                remainingSeconds =
                                        Math.max(
                                                remainingSeconds,
                                                1
                                        );

                                throw new IllegalStateException(
                                        "Please wait "
                                                + remainingSeconds
                                                + " seconds before requesting another OTP"
                                );
                            }
                        }
                );
    }

    private String normalizeEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Email address is required"
            );
        }

        String normalizedEmail =
                email.trim()
                        .toLowerCase(Locale.ROOT);

        if (!normalizedEmail.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        )) {

            throw new IllegalArgumentException(
                    "Invalid email address"
            );
        }

        return normalizedEmail;
    }

    private String normalizeUserName(
            String userName
    ) {

        if (userName == null
                || userName.isBlank()) {

            return "User";
        }

        return userName.trim();
    }

    private String normalizeOtp(
            String otp
    ) {

        if (otp == null
                || otp.isBlank()) {

            throw new IllegalArgumentException(
                    "OTP is required"
            );
        }

        String normalizedOtp =
                otp.trim();

        if (!normalizedOtp.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must contain exactly 6 digits"
            );
        }

        return normalizedOtp;
    }

    private void validateGeneratedOtp(
            String otp
    ) {

        if (otp == null
                || !otp.matches("\\d{6}")) {

            throw new IllegalStateException(
                    "OTP generator must generate exactly six digits"
            );
        }
    }

    private void validatePurpose(
            OtpPurpose purpose
    ) {

        if (purpose == null) {

            throw new IllegalArgumentException(
                    "OTP purpose is required"
            );
        }
    }
}