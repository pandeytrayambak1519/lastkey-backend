package com.lastkey.backend.email.controller;

import com.lastkey.backend.email.dto.OtpRequest;
import com.lastkey.backend.email.dto.OtpResponse;
import com.lastkey.backend.email.enums.OtpPurpose;
import com.lastkey.backend.email.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
public class EmailVerificationController {

    private final OtpService otpService;

    public EmailVerificationController(
            OtpService otpService
    ) {
        this.otpService = otpService;
    }

    // =========================================================
    // Send OTP
    // =========================================================

    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(

            @Valid
            @RequestBody
            OtpRequest request
    ) {

        validateSendOtpRequest(request);

        otpService.generateAndSendOtp(
                request.getEmail(),
                request.getUserName(),
                request.getPurpose()
        );

        OtpResponse response =
                OtpResponse.builder()
                        .success(true)
                        .message(
                                "OTP has been sent successfully to "
                                        + request.getEmail()
                        )
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Verify OTP
    // =========================================================

    @PostMapping("/verify-otp")
    public ResponseEntity<OtpResponse> verifyOtp(

            @Valid
            @RequestBody
            OtpRequest request
    ) {

        validateVerifyOtpRequest(request);

        boolean verified =
                otpService.verifyOtp(
                        request.getEmail(),
                        request.getOtp(),
                        request.getPurpose()
                );

        OtpResponse response =
                OtpResponse.builder()
                        .success(verified)
                        .message(
                                verified
                                        ? "OTP verified successfully"
                                        : "The provided OTP is invalid"
                        )
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Check OTP verification status
    // =========================================================

    @GetMapping("/verified")
    public ResponseEntity<OtpResponse> isOtpVerified(

            @RequestParam
            String email,

            @RequestParam
            OtpPurpose purpose
    ) {

        boolean verified =
                otpService.isOtpVerified(
                        email,
                        purpose
                );

        OtpResponse response =
                OtpResponse.builder()
                        .success(verified)
                        .message(
                                verified
                                        ? "OTP has already been verified"
                                        : "OTP has not been verified"
                        )
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Invalidate active OTP
    // =========================================================

    @DeleteMapping("/invalidate")
    public ResponseEntity<OtpResponse> invalidateOtp(

            @RequestParam
            String email,

            @RequestParam
            OtpPurpose purpose
    ) {

        otpService.invalidateOtps(
                email,
                purpose
        );

        OtpResponse response =
                OtpResponse.builder()
                        .success(true)
                        .message("Active OTP invalidated successfully")
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Request validation helpers
    // =========================================================

    private void validateSendOtpRequest(
            OtpRequest request
    ) {

        if (request.getUserName() == null
                || request.getUserName().isBlank()) {

            throw new IllegalArgumentException(
                    "User name is required while sending OTP"
            );
        }

        if (request.getPurpose() == OtpPurpose.PASSWORD_RESET) {

            throw new IllegalArgumentException(
                    "Use FORGOT_PASSWORD purpose to request a password-reset OTP"
            );
        }
    }

    private void validateVerifyOtpRequest(
            OtpRequest request
    ) {

        if (request.getOtp() == null
                || request.getOtp().isBlank()) {

            throw new IllegalArgumentException(
                    "OTP is required for verification"
            );
        }

        if (!request.getOtp().trim().matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must contain exactly 6 digits"
            );
        }
    }
}