package com.lastkey.backend.emergency.service.impl;

import com.lastkey.backend.email.service.EmailService;
import com.lastkey.backend.emergency.entity.EmergencyOtp;
import com.lastkey.backend.emergency.entity.EmergencyRequest;
import com.lastkey.backend.emergency.enums.EmergencyStatus;
import com.lastkey.backend.emergency.exception.EmergencyAccessDeniedException;
import com.lastkey.backend.emergency.exception.EmergencyRequestNotFoundException;
import com.lastkey.backend.emergency.exception.InvalidEmergencyStatusException;
import com.lastkey.backend.emergency.repository.EmergencyOtpRepository;
import com.lastkey.backend.emergency.repository.EmergencyRequestRepository;
import com.lastkey.backend.emergency.service.EmergencyOtpService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmergencyOtpServiceImpl
        implements EmergencyOtpService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final EmergencyOtpRepository
            emergencyOtpRepository;

    private final EmergencyRequestRepository
            emergencyRequestRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    @Value(
            "${app.security.emergency-otp.expiration-minutes:10}"
    )
    private int otpExpirationMinutes;

    @Value(
            "${app.security.emergency-otp.maximum-attempts:5}"
    )
    private int maximumAttempts;

    @Value(
            "${app.security.emergency-otp.resend-cooldown-seconds:60}"
    )
    private long resendCooldownSeconds;

    @Override
    public void sendOtp(
            UUID emergencyRequestId,
            String currentUserEmail
    ) {

        User currentUser =
                getCurrentUser(
                        currentUserEmail
                );

        EmergencyRequest emergencyRequest =
                getEmergencyRequest(
                        emergencyRequestId
                );

        validateOwnerAccess(
                emergencyRequest,
                currentUser
        );

        validateOtpCanBeSent(
                emergencyRequest
        );

        validateResendCooldown(
                emergencyRequest
        );

        LocalDateTime now =
                LocalDateTime.now();

        emergencyOtpRepository.invalidateActiveOtps(
                emergencyRequest,
                now
        );

        String rawOtp =
                generateSixDigitOtp();

        EmergencyOtp emergencyOtp =
                EmergencyOtp.builder()
                        .emergencyRequest(
                                emergencyRequest
                        )
                        .otpHash(
                                passwordEncoder.encode(
                                        rawOtp
                                )
                        )
                        .verificationAttempts(0)
                        .maximumAttempts(
                                maximumAttempts
                        )
                        .verified(false)
                        .invalidated(false)
                        .expiresAt(
                                now.plusMinutes(
                                        otpExpirationMinutes
                                )
                        )
                        .build();

        emergencyOtpRepository.save(
                emergencyOtp
        );

        emailService.sendOtpEmail(
                currentUser.getEmail(),
                getFullName(currentUser),
                rawOtp,
                otpExpirationMinutes
        );
    }

    @Override
    public void verifyOtp(
            UUID emergencyRequestId,
            String otp,
            String currentUserEmail
    ) {

        User currentUser =
                getCurrentUser(
                        currentUserEmail
                );

        EmergencyRequest emergencyRequest =
                getEmergencyRequest(
                        emergencyRequestId
                );

        validateOwnerAccess(
                emergencyRequest,
                currentUser
        );

        validateOtpCanBeVerified(
                emergencyRequest
        );

        String normalizedOtp =
                normalizeAndValidateOtp(
                        otp
                );

        EmergencyOtp emergencyOtp =
                emergencyOtpRepository
                        .findFirstByEmergencyRequestAndVerifiedFalseAndInvalidatedFalseOrderByCreatedAtDesc(
                                emergencyRequest
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No active emergency OTP was found"
                                        )
                        );

        if (emergencyOtp.isExpired()) {

            emergencyOtp.invalidate();

            emergencyOtpRepository.save(
                    emergencyOtp
            );

            throw new IllegalArgumentException(
                    "Emergency OTP has expired"
            );
        }

        if (!emergencyOtp.hasAttemptsRemaining()) {

            emergencyOtp.invalidate();

            emergencyOtpRepository.save(
                    emergencyOtp
            );

            throw new IllegalArgumentException(
                    "Maximum OTP verification attempts exceeded"
            );
        }

        boolean matches =
                passwordEncoder.matches(
                        normalizedOtp,
                        emergencyOtp.getOtpHash()
                );

        if (!matches) {

            emergencyOtp.registerFailedAttempt();

            emergencyOtpRepository.save(
                    emergencyOtp
            );

            int remainingAttempts =
                    Math.max(
                            emergencyOtp.getMaximumAttempts()
                                    - emergencyOtp
                                    .getVerificationAttempts(),
                            0
                    );

            throw new IllegalArgumentException(
                    "Invalid emergency OTP. Remaining attempts: "
                            + remainingAttempts
            );
        }

        emergencyOtp.markVerified();

        emergencyOtpRepository.save(
                emergencyOtp
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOtpVerified(
            UUID emergencyRequestId
    ) {

        EmergencyRequest emergencyRequest =
                getEmergencyRequest(
                        emergencyRequestId
                );

        return emergencyOtpRepository
                .existsByEmergencyRequestAndVerifiedTrueAndInvalidatedFalse(
                        emergencyRequest
                );
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOtpVerification(
            UUID emergencyRequestId
    ) {

        if (!isOtpVerified(
                emergencyRequestId
        )) {

            throw new EmergencyAccessDeniedException(
                    "Emergency OTP verification is required "
                            + "before approving or releasing documents"
            );
        }
    }

    @Override
    public void invalidateOtps(
            UUID emergencyRequestId
    ) {

        EmergencyRequest emergencyRequest =
                getEmergencyRequest(
                        emergencyRequestId
                );

        emergencyOtpRepository.invalidateActiveOtps(
                emergencyRequest,
                LocalDateTime.now()
        );
    }

    private User getCurrentUser(
            String currentUserEmail
    ) {

        if (currentUserEmail == null
                || currentUserEmail.isBlank()) {

            throw new UsernameNotFoundException(
                    "Authenticated user email is missing"
            );
        }

        String normalizedEmail =
                currentUserEmail
                        .trim()
                        .toLowerCase();

        return userRepository
                .findByEmail(
                        normalizedEmail
                )
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "User not found with email: "
                                                + normalizedEmail
                                )
                );
    }

    private EmergencyRequest getEmergencyRequest(
            UUID emergencyRequestId
    ) {

        if (emergencyRequestId == null) {

            throw new IllegalArgumentException(
                    "Emergency request ID is required"
            );
        }

        return emergencyRequestRepository
                .findById(
                        emergencyRequestId
                )
                .orElseThrow(
                        () ->
                                new EmergencyRequestNotFoundException(
                                        "Emergency request not found with ID: "
                                                + emergencyRequestId
                                )
                );
    }

    private void validateOwnerAccess(
            EmergencyRequest emergencyRequest,
            User currentUser
    ) {

        if (emergencyRequest.getOwner() == null
                || emergencyRequest
                .getOwner()
                .getId() == null
                || currentUser.getId() == null
                || !emergencyRequest
                .getOwner()
                .getId()
                .equals(
                        currentUser.getId()
                )) {

            throw new EmergencyAccessDeniedException(
                    "Only the emergency-request owner "
                            + "can manage its OTP"
            );
        }
    }

    private void validateOtpCanBeSent(
            EmergencyRequest emergencyRequest
    ) {

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new InvalidEmergencyStatusException(
                    "OTP cannot be sent for an inactive emergency request"
            );
        }

        EmergencyStatus status =
                emergencyRequest.getStatus();

        if (status != EmergencyStatus.PENDING
                && status != EmergencyStatus.OWNER_NOTIFIED
                && status != EmergencyStatus.WAITING_PERIOD) {

            throw new InvalidEmergencyStatusException(
                    "OTP cannot be sent when emergency status is "
                            + status
            );
        }
    }

    private void validateOtpCanBeVerified(
            EmergencyRequest emergencyRequest
    ) {

        if (Boolean.FALSE.equals(
                emergencyRequest.getActive()
        )) {

            throw new InvalidEmergencyStatusException(
                    "OTP cannot be verified for an inactive request"
            );
        }

        EmergencyStatus status =
                emergencyRequest.getStatus();

        if (status != EmergencyStatus.PENDING
                && status != EmergencyStatus.OWNER_NOTIFIED
                && status != EmergencyStatus.WAITING_PERIOD) {

            throw new InvalidEmergencyStatusException(
                    "OTP cannot be verified when emergency status is "
                            + status
            );
        }
    }

    private void validateResendCooldown(
            EmergencyRequest emergencyRequest
    ) {

        emergencyOtpRepository
                .findFirstByEmergencyRequestOrderByCreatedAtDesc(
                        emergencyRequest
                )
                .ifPresent(
                        previousOtp -> {

                            if (previousOtp.getCreatedAt() == null) {
                                return;
                            }

                            long elapsedSeconds =
                                    Duration.between(
                                            previousOtp.getCreatedAt(),
                                            LocalDateTime.now()
                                    ).getSeconds();

                            if (elapsedSeconds
                                    < resendCooldownSeconds) {

                                long remainingSeconds =
                                        resendCooldownSeconds
                                                - elapsedSeconds;

                                throw new IllegalArgumentException(
                                        "Please wait "
                                                + remainingSeconds
                                                + " second(s) before requesting another OTP"
                                );
                            }
                        }
                );
    }

    private String generateSixDigitOtp() {

        int otp =
                100000
                        + SECURE_RANDOM.nextInt(
                        900000
                );

        return String.valueOf(
                otp
        );
    }

    private String normalizeAndValidateOtp(
            String otp
    ) {

        if (otp == null
                || otp.isBlank()) {

            throw new IllegalArgumentException(
                    "Emergency OTP is required"
            );
        }

        String normalizedOtp =
                otp.trim();

        if (!normalizedOtp.matches(
                "\\d{6}"
        )) {

            throw new IllegalArgumentException(
                    "Emergency OTP must contain exactly 6 digits"
            );
        }

        return normalizedOtp;
    }

    private String getFullName(
            User user
    ) {

        String firstName =
                normalizeNullable(
                        user.getFirstName()
                );

        String lastName =
                normalizeNullable(
                        user.getLastName()
                );

        String fullName =
                String.join(
                        " ",
                        firstName == null
                                ? ""
                                : firstName,

                        lastName == null
                                ? ""
                                : lastName
                ).trim();

        return fullName.isBlank()
                ? "LastKey User"
                : fullName;
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}