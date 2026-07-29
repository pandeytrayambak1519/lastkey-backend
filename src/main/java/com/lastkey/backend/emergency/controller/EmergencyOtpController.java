package com.lastkey.backend.emergency.controller;

import com.lastkey.backend.emergency.service.EmergencyOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/emergencies/{emergencyRequestId}/otp"
)
@RequiredArgsConstructor
public class EmergencyOtpController {

    private final EmergencyOtpService
            emergencyOtpService;

    /*
     * POST:
     * /api/v1/emergencies/{emergencyRequestId}/otp/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>>
    sendEmergencyOtp(

            @PathVariable
            UUID emergencyRequestId,

            Authentication authentication
    ) {

        emergencyOtpService.sendOtp(
                emergencyRequestId,
                getAuthenticatedEmail(
                        authentication
                )
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Emergency verification OTP was sent successfully"
                )
        );
    }

    /*
     * POST:
     * /api/v1/emergencies/{emergencyRequestId}/otp/verify?otp=123456
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>>
    verifyEmergencyOtp(

            @PathVariable
            UUID emergencyRequestId,

            @RequestParam
            String otp,

            Authentication authentication
    ) {

        emergencyOtpService.verifyOtp(
                emergencyRequestId,
                otp,
                getAuthenticatedEmail(
                        authentication
                )
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Emergency OTP verified successfully"
                )
        );
    }

    /*
     * GET:
     * /api/v1/emergencies/{emergencyRequestId}/otp/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>>
    getEmergencyOtpStatus(

            @PathVariable
            UUID emergencyRequestId,

            Authentication authentication
    ) {

        /*
         * The authenticated email is validated before returning
         * security-related information.
         */
        getAuthenticatedEmail(
                authentication
        );

        boolean verified =
                emergencyOtpService.isOtpVerified(
                        emergencyRequestId
                );

        return ResponseEntity.ok(
                Map.of(
                        "verified",
                        verified
                )
        );
    }

    private String getAuthenticatedEmail(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication
                .getName()
                .isBlank()) {

            throw new IllegalStateException(
                    "Authenticated user information is unavailable"
            );
        }

        return authentication.getName();
    }
}