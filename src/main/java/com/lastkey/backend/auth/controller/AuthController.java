package com.lastkey.backend.auth.controller;

import com.lastkey.backend.auth.dto.request.LoginRequest;
import com.lastkey.backend.auth.dto.request.RefreshTokenRequest;
import com.lastkey.backend.auth.dto.request.RegisterRequest;
import com.lastkey.backend.auth.dto.request.ResendOtpRequest;
import com.lastkey.backend.auth.dto.request.VerifyEmailRequest;
import com.lastkey.backend.auth.dto.response.EmailVerificationResponse;
import com.lastkey.backend.auth.dto.response.LoginResponse;
import com.lastkey.backend.auth.dto.response.RegisterResponse;
import com.lastkey.backend.auth.service.AuthService;
import com.lastkey.backend.auth.service.EmailVerificationService;
import com.lastkey.backend.email.dto.OtpRequest;
import com.lastkey.backend.email.dto.OtpResponse;
import com.lastkey.backend.email.dto.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = """
                Registration, login, token refresh, logout,
                email verification and password recovery APIs.
                """
)
public class AuthController {

    private final AuthService authService;

    private final EmailVerificationService emailVerificationService;

    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    @Operation(
            summary = "Register new user",
            description = """
                    Creates a new LastKey user account and sends
                    an email-verification OTP to the registered email.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration details."
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email address or phone number is already registered."
            )
    })
    public ResponseEntity<RegisterResponse> register(

            @Valid
            @RequestBody
            RegisterRequest request
    ) {

        RegisterResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = """
                    Authenticates the user using email and password
                    and returns access and refresh tokens.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid login request."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password."
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User account is disabled, locked or not verified."
            )
    })
    public ResponseEntity<LoginResponse> login(

            @Valid
            @RequestBody
            LoginRequest request
    ) {

        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Refresh access token",
            description = """
                    Generates a new access token and refresh token
                    using a valid and active refresh token.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tokens refreshed successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token is missing or invalid."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token is expired or revoked."
            )
    })
    public ResponseEntity<LoginResponse> refreshToken(

            @Valid
            @RequestBody
            RefreshTokenRequest request
    ) {

        LoginResponse response =
                authService.refreshToken(
                        request.getRefreshToken()
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = """
                    Revokes the supplied refresh token and ends
                    the associated authenticated session.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token is missing or invalid."
            )
    })
    public ResponseEntity<Map<String, String>> logout(

            @Valid
            @RequestBody
            RefreshTokenRequest request
    ) {

        authService.logout(
                request.getRefreshToken()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logout successful."
                )
        );
    }

    // =========================================================
    // VERIFY REGISTRATION EMAIL
    // =========================================================

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verify registration email",
            description = """
                    Verifies the registered user's email address
                    using the six-digit verification OTP.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Email verified successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "OTP is invalid, expired or already used."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User account was not found."
            )
    })
    public ResponseEntity<EmailVerificationResponse> verifyEmail(

            @Valid
            @RequestBody
            VerifyEmailRequest request
    ) {

        EmailVerificationResponse response =
                emailVerificationService.verifyEmail(
                        request
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // RESEND REGISTRATION VERIFICATION OTP
    // =========================================================

    @PostMapping("/resend-verification-otp")
    @Operation(
            summary = "Resend email-verification OTP",
            description = """
                    Generates and sends a new email-verification
                    OTP to the registered user's email address.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Verification OTP sent successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request is invalid or resend limit has been reached."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User account was not found."
            )
    })
    public ResponseEntity<EmailVerificationResponse> resendOtp(

            @Valid
            @RequestBody
            ResendOtpRequest request
    ) {

        EmailVerificationResponse response =
                emailVerificationService.resendOtp(
                        request
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password-reset OTP",
            description = """
                    Sends a password-reset OTP to the user's
                    registered email address.

                    For security reasons, the response does not reveal
                    whether the supplied email address exists.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password-reset request processed successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email address is missing or invalid."
            )
    })
    public ResponseEntity<OtpResponse> forgotPassword(

            @Valid
            @RequestBody
            OtpRequest request
    ) {

        authService.forgotPassword(request);

        OtpResponse response =
                OtpResponse.builder()
                        .success(true)
                        .message(
                                "If the email is registered, "
                                        + "a password-reset OTP "
                                        + "has been sent."
                        )
                        .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset forgotten password",
            description = """
                    Verifies the password-reset OTP and updates
                    the user's account password.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "OTP is invalid, expired or the new password is invalid."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User account was not found."
            )
    })
    public ResponseEntity<OtpResponse> resetPassword(

            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {

        authService.resetPassword(request);

        OtpResponse response =
                OtpResponse.builder()
                        .success(true)
                        .message(
                                "Password reset successfully. "
                                        + "Please log in using your "
                                        + "new password."
                        )
                        .build();

        return ResponseEntity.ok(response);
    }
}