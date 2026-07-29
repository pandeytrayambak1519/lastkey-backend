package com.lastkey.backend.auth.service.impl;

import com.lastkey.backend.auth.dto.request.LoginRequest;
import com.lastkey.backend.auth.dto.request.RegisterRequest;
import com.lastkey.backend.auth.dto.response.LoginResponse;
import com.lastkey.backend.auth.dto.response.RegisterResponse;
import com.lastkey.backend.auth.entity.RefreshToken;
import com.lastkey.backend.auth.exception.AccountLockedException;
import com.lastkey.backend.auth.repository.RefreshTokenRepository;
import com.lastkey.backend.auth.service.AuthService;
import com.lastkey.backend.auth.service.EmailVerificationService;
import com.lastkey.backend.common.enums.AccountStatus;
import com.lastkey.backend.common.enums.RoleType;
import com.lastkey.backend.email.dto.OtpRequest;
import com.lastkey.backend.email.dto.ResetPasswordRequest;
import com.lastkey.backend.email.enums.OtpPurpose;
import com.lastkey.backend.email.service.EmailService;
import com.lastkey.backend.email.service.OtpService;
import com.lastkey.backend.role.entity.Role;
import com.lastkey.backend.role.repository.RoleRepository;
import com.lastkey.backend.security.jwt.JwtService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final EmailVerificationService emailVerificationService;

    private final OtpService otpService;

    private final EmailService emailService;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private long accountLockDurationMinutes;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailVerificationService emailVerificationService,
            OtpService otpService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    // =========================================================
    // Register a new user
    // =========================================================

    @Override
    @Transactional
    public RegisterResponse register(
            RegisterRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Registration request is required"
            );
        }

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        String normalizedPhone =
                normalizePhone(request.getPhone());

        log.info(
                "Registration attempt received: email={}",
                normalizedEmail
        );

        if (userRepository.existsByEmail(
                normalizedEmail
        )) {

            log.warn(
                    "Registration rejected because email already exists: email={}",
                    normalizedEmail
            );

            throw new IllegalArgumentException(
                    "An account already exists with this email"
            );
        }

        if (userRepository.existsByPhone(
                normalizedPhone
        )) {

            log.warn(
                    "Registration rejected because phone number already exists"
            );

            throw new IllegalArgumentException(
                    "An account already exists with this phone number"
            );
        }

        Role defaultRole =
                roleRepository
                        .findByName(RoleType.USER)
                        .orElseThrow(
                                () -> {
                                    log.error(
                                            "Default USER role is missing from database"
                                    );

                                    return new IllegalStateException(
                                            "Default USER role is not available in database"
                                    );
                                }
                        );

        User user =
                User.builder()
                        .firstName(
                                normalizeRequiredText(
                                        request.getFirstName(),
                                        "First name"
                                )
                        )
                        .lastName(
                                normalizeRequiredText(
                                        request.getLastName(),
                                        "Last name"
                                )
                        )
                        .email(normalizedEmail)
                        .phone(normalizedPhone)
                        .password(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )
                        .emailVerified(false)
                        .accountStatus(AccountStatus.ACTIVE)
                        .accountLocked(false)
                        .failedLoginAttempts(0)
                        .accountLockedUntil(null)
                        .role(defaultRole)
                        .build();

        User savedUser =
                userRepository.save(user);

        log.info(
                "User registered successfully: userId={}, email={}",
                savedUser.getId(),
                savedUser.getEmail()
        );

        /*
         * Existing email-verification system.
         */
        emailVerificationService
                .createAndSendOtp(savedUser);

        log.info(
                "Email verification OTP generated for userId={}",
                savedUser.getId()
        );

        /*
         * Welcome email failure should not roll back registration.
         */
        try {

            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    buildFullName(savedUser)
            );

            log.info(
                    "Welcome email sent successfully: userId={}",
                    savedUser.getId()
            );

        } catch (RuntimeException exception) {

            log.warn(
                    "Welcome email could not be sent: userId={}, reason={}",
                    savedUser.getId(),
                    exception.getMessage()
            );
        }

        return RegisterResponse.builder()
                .message(
                        "Registration successful. "
                                + "Verification OTP sent to email"
                )
                .email(savedUser.getEmail())
                .emailVerified(
                        Boolean.TRUE.equals(
                                savedUser.getEmailVerified()
                        )
                )
                .build();
    }

    // =========================================================
    // Login
    // =========================================================

    @Override
    @Transactional
    public LoginResponse login(
            LoginRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Login request is required"
            );
        }

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        log.info(
                "Login attempt received: email={}",
                normalizedEmail
        );

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(
                                () -> {
                                    log.warn(
                                            "Login failed for unknown email={}",
                                            normalizedEmail
                                    );

                                    return new BadCredentialsException(
                                            "Invalid email or password"
                                    );
                                }
                        );

        validateAccountForAuthentication(user);

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            log.warn(
                    "Login failed because of invalid credentials: userId={}, email={}",
                    user.getId(),
                    user.getEmail()
            );

            handleFailedLogin(user);

            int remainingAttempts =
                    Math.max(
                            maxFailedLoginAttempts
                                    - user.getFailedLoginAttempts(),
                            0
                    );

            if (Boolean.TRUE.equals(
                    user.getAccountLocked()
            )) {

                log.error(
                        "Account locked after repeated failed login attempts: "
                                + "userId={}, email={}, lockedUntil={}",
                        user.getId(),
                        user.getEmail(),
                        user.getAccountLockedUntil()
                );

                throw new AccountLockedException(
                        "Maximum login attempts exceeded. "
                                + "Your account is locked until "
                                + user.getAccountLockedUntil(),
                        user.getAccountLockedUntil()
                );
            }

            throw new BadCredentialsException(
                    "Invalid email or password. Remaining attempts: "
                            + remainingAttempts
            );
        }

        /*
         * Enable this block when email verification
         * must be compulsory before login.
         */

//        if (!Boolean.TRUE.equals(
//                user.getEmailVerified()
//        )) {
//
//            log.warn(
//                    "Login rejected because email is not verified: userId={}",
//                    user.getId()
//            );
//
//            throw new IllegalStateException(
//                    "Please verify your email before logging in"
//            );
//        }

        resetLoginSecurityState(user);

        user.setLastLoginAt(
                LocalDateTime.now()
        );

        userRepository.save(user);

        UserDetails userDetails =
                createUserDetails(user);

        String accessToken =
                jwtService.generateAccessToken(
                        userDetails
                );

        String refreshToken =
                jwtService.generateRefreshToken(
                        userDetails
                );

        saveOrUpdateRefreshToken(
                user,
                refreshToken
        );

        log.info(
                "User logged in successfully: userId={}, email={}, role={}",
                user.getId(),
                user.getEmail(),
                user.getRole().getName()
        );

        return buildLoginResponse(
                user,
                accessToken,
                refreshToken
        );
    }

    // =========================================================
    // Refresh authentication tokens
    // =========================================================

    @Override
    @Transactional
    public LoginResponse refreshToken(
            String refreshTokenValue
    ) {

        log.info(
                "Refresh-token request received"
        );

        if (refreshTokenValue == null
                || refreshTokenValue.isBlank()) {

            log.warn(
                    "Refresh-token request rejected because token is missing"
            );

            throw new IllegalArgumentException(
                    "Refresh token is required"
            );
        }

        String normalizedToken =
                refreshTokenValue.trim();

        RefreshToken storedRefreshToken =
                refreshTokenRepository
                        .findByToken(normalizedToken)
                        .orElseThrow(
                                () -> {
                                    log.warn(
                                            "Refresh-token request rejected because token was not found"
                                    );

                                    return new IllegalArgumentException(
                                            "Invalid refresh token"
                                    );
                                }
                        );

        if (Boolean.TRUE.equals(
                storedRefreshToken.getRevoked()
        )) {

            log.warn(
                    "Revoked refresh token was used: refreshTokenId={}",
                    storedRefreshToken.getId()
            );

            throw new IllegalArgumentException(
                    "Refresh token has been revoked"
            );
        }

        if (storedRefreshToken.getExpiryDate() == null
                || storedRefreshToken
                .getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            storedRefreshToken.setRevoked(true);

            refreshTokenRepository.save(
                    storedRefreshToken
            );

            log.warn(
                    "Expired refresh token was used: refreshTokenId={}",
                    storedRefreshToken.getId()
            );

            throw new IllegalArgumentException(
                    "Refresh token has expired. Please login again"
            );
        }

        User user =
                storedRefreshToken.getUser();

        if (user == null) {

            log.error(
                    "Refresh token has no associated user: refreshTokenId={}",
                    storedRefreshToken.getId()
            );

            throw new IllegalStateException(
                    "Refresh token is not associated with a user"
            );
        }

        validateAccountForAuthentication(user);

        UserDetails userDetails =
                createUserDetails(user);

        try {

            if (!jwtService.isTokenValid(
                    normalizedToken,
                    userDetails
            )) {

                throw new IllegalArgumentException(
                        "Invalid refresh token"
                );
            }

        } catch (RuntimeException exception) {

            storedRefreshToken.setRevoked(true);

            refreshTokenRepository.save(
                    storedRefreshToken
            );

            log.warn(
                    "Refresh token validation failed: userId={}, reason={}",
                    user.getId(),
                    exception.getMessage()
            );

            throw new IllegalArgumentException(
                    "Invalid or expired refresh token",
                    exception
            );
        }

        String newAccessToken =
                jwtService.generateAccessToken(
                        userDetails
                );

        String newRefreshToken =
                jwtService.generateRefreshToken(
                        userDetails
                );

        /*
         * Refresh-token rotation.
         */
        storedRefreshToken.setToken(
                newRefreshToken
        );

        storedRefreshToken.setExpiryDate(
                calculateRefreshTokenExpiry()
        );

        storedRefreshToken.setRevoked(false);

        refreshTokenRepository.save(
                storedRefreshToken
        );

        log.info(
                "Refresh token rotated successfully: userId={}, refreshTokenId={}",
                user.getId(),
                storedRefreshToken.getId()
        );

        return buildLoginResponse(
                user,
                newAccessToken,
                newRefreshToken
        );
    }

    // =========================================================
    // Logout
    // =========================================================

    @Override
    @Transactional
    public void logout(
            String refreshTokenValue
    ) {

        log.info(
                "Logout request received"
        );

        if (refreshTokenValue == null
                || refreshTokenValue.isBlank()) {

            log.warn(
                    "Logout request rejected because refresh token is missing"
            );

            throw new IllegalArgumentException(
                    "Refresh token is required"
            );
        }

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(
                                refreshTokenValue.trim()
                        )
                        .orElseThrow(
                                () -> {
                                    log.warn(
                                            "Logout request rejected because refresh token was not found"
                                    );

                                    return new IllegalArgumentException(
                                            "Invalid refresh token"
                                    );
                                }
                        );

        if (Boolean.TRUE.equals(
                refreshToken.getRevoked()
        )) {

            log.warn(
                    "Logout attempted with an already revoked token: refreshTokenId={}",
                    refreshToken.getId()
            );

            throw new IllegalArgumentException(
                    "Refresh token is already revoked"
            );
        }

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(
                refreshToken
        );

        User user =
                refreshToken.getUser();

        if (user != null) {

            log.info(
                    "User logged out successfully: userId={}, email={}",
                    user.getId(),
                    user.getEmail()
            );

        } else {

            log.info(
                    "Refresh token revoked successfully: refreshTokenId={}",
                    refreshToken.getId()
            );
        }
    }

    // =========================================================
    // Forgot password
    // =========================================================

    @Override
    @Transactional
    public void forgotPassword(
            OtpRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Forgot-password request is required"
            );
        }

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        log.info(
                "Forgot-password request received: email={}",
                normalizedEmail
        );

        /*
         * Do not expose whether an email is registered.
         * The controller always returns the same generic response.
         */
        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElse(null);

        if (user == null) {

            log.warn(
                    "Forgot-password request received for an unregistered email={}",
                    normalizedEmail
            );

            return;
        }

        if (user.getAccountStatus()
                != AccountStatus.ACTIVE) {

            log.warn(
                    "Forgot-password request ignored because account is not active: "
                            + "userId={}, status={}",
                    user.getId(),
                    user.getAccountStatus()
            );

            return;
        }

        /*
         * Password reset is allowed for temporarily locked accounts,
         * so the user can recover access.
         */
        otpService.generateAndSendOtp(
                user.getEmail(),
                buildFullName(user),
                OtpPurpose.FORGOT_PASSWORD
        );

        log.info(
                "Password-reset OTP generated successfully: userId={}",
                user.getId()
        );
    }

    // =========================================================
    // Reset password
    // =========================================================

    @Override
    @Transactional
    public void resetPassword(
            ResetPasswordRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Reset-password request is required"
            );
        }

        validatePasswordResetRequest(request);

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        log.info(
                "Password-reset attempt received: email={}",
                normalizedEmail
        );

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(
                                () -> {
                                    log.warn(
                                            "Password-reset request rejected because user does not exist"
                                    );

                                    return new IllegalArgumentException(
                                            "Invalid password-reset request"
                                    );
                                }
                        );

        if (user.getAccountStatus()
                != AccountStatus.ACTIVE) {

            log.warn(
                    "Password-reset request rejected because account is not active: "
                            + "userId={}, status={}",
                    user.getId(),
                    user.getAccountStatus()
            );

            throw new IllegalStateException(
                    "The account is not active"
            );
        }

        boolean otpVerified =
                otpService.verifyOtp(
                        normalizedEmail,
                        request.getOtp(),
                        OtpPurpose.FORGOT_PASSWORD
                );

        if (!otpVerified) {

            log.warn(
                    "Password-reset request rejected because OTP verification failed: userId={}",
                    user.getId()
            );

            throw new IllegalArgumentException(
                    "The provided OTP is invalid"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {

            log.warn(
                    "Password-reset request rejected because new password matches current password: "
                            + "userId={}",
                    user.getId()
            );

            throw new IllegalArgumentException(
                    "New password must be different from the current password"
            );
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        /*
         * Unlock account after successful password reset.
         */
        resetLoginSecurityState(user);

        userRepository.save(user);

        /*
         * Revoke current refresh token so previously
         * issued sessions cannot continue.
         */
        revokeUserRefreshToken(user);

        /*
         * Invalidate any remaining forgot-password OTP.
         */
        otpService.invalidateOtps(
                normalizedEmail,
                OtpPurpose.FORGOT_PASSWORD
        );

        log.info(
                "Password reset successfully: userId={}, email={}",
                user.getId(),
                user.getEmail()
        );

        /*
         * Password has already been updated, so an email
         * failure should not roll back the password change.
         */
        try {

            emailService.sendPasswordChangedEmail(
                    user.getEmail(),
                    buildFullName(user)
            );

            log.info(
                    "Password-changed confirmation email sent: userId={}",
                    user.getId()
            );

        } catch (RuntimeException exception) {

            log.warn(
                    "Password-changed email could not be sent: userId={}, reason={}",
                    user.getId(),
                    exception.getMessage()
            );
        }
    }

    // =========================================================
    // Save or update refresh token
    // =========================================================

    private void saveOrUpdateRefreshToken(
            User user,
            String tokenValue
    ) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByUser(user)
                        .orElseGet(
                                RefreshToken::new
                        );

        refreshToken.setToken(tokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(
                calculateRefreshTokenExpiry()
        );
        refreshToken.setRevoked(false);

        RefreshToken savedRefreshToken =
                refreshTokenRepository.save(
                        refreshToken
                );

        log.debug(
                "Refresh token saved successfully: userId={}, refreshTokenId={}",
                user.getId(),
                savedRefreshToken.getId()
        );
    }

    // =========================================================
    // Revoke user's current refresh token
    // =========================================================

    private void revokeUserRefreshToken(
            User user
    ) {

        refreshTokenRepository
                .findByUser(user)
                .ifPresent(
                        refreshToken -> {

                            refreshToken.setRevoked(true);

                            refreshTokenRepository.save(
                                    refreshToken
                            );

                            log.info(
                                    "User refresh token revoked: userId={}, refreshTokenId={}",
                                    user.getId(),
                                    refreshToken.getId()
                            );
                        }
                );
    }

    // =========================================================
    // Create Spring Security UserDetails
    // =========================================================

    private UserDetails createUserDetails(
            User user
    ) {

        if (user.getRole() == null
                || user.getRole().getName() == null) {

            log.error(
                    "User role information is unavailable: userId={}",
                    user.getId()
            );

            throw new IllegalStateException(
                    "User role information is unavailable"
            );
        }

        String authority =
                "ROLE_"
                        + user.getRole()
                        .getName()
                        .name();

        boolean accountEnabled =
                user.getAccountStatus()
                        == AccountStatus.ACTIVE;

        boolean accountNonLocked =
                !Boolean.TRUE.equals(
                        user.getAccountLocked()
                );

        return new org.springframework.security
                .core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                accountEnabled,
                true,
                true,
                accountNonLocked,
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                authority
                        )
                )
        );
    }

    // =========================================================
    // Build login response
    // =========================================================

    private LoginResponse buildLoginResponse(
            User user,
            String accessToken,
            String refreshToken
    ) {

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(
                        user.getRole()
                                .getName()
                                .name()
                )
                .emailVerified(
                        Boolean.TRUE.equals(
                                user.getEmailVerified()
                        )
                )
                .build();
    }

    // =========================================================
    // Handle failed login
    // =========================================================

    private void handleFailedLogin(
            User user
    ) {

        validateLoginSecurityConfiguration();

        int currentAttempts =
                user.getFailedLoginAttempts() == null
                        ? 0
                        : user.getFailedLoginAttempts();

        int updatedAttempts =
                currentAttempts + 1;

        user.setFailedLoginAttempts(
                updatedAttempts
        );

        log.warn(
                "Failed login attempt recorded: userId={}, attempt={}, maximumAttempts={}",
                user.getId(),
                updatedAttempts,
                maxFailedLoginAttempts
        );

        if (updatedAttempts >= maxFailedLoginAttempts) {

            LocalDateTime lockedUntil =
                    LocalDateTime.now()
                            .plusMinutes(
                                    accountLockDurationMinutes
                            );

            user.setAccountLocked(true);
            user.setAccountLockedUntil(
                    lockedUntil
            );

            revokeUserRefreshToken(user);

            log.error(
                    "Account locked because maximum failed login attempts were reached: "
                            + "userId={}, lockedUntil={}",
                    user.getId(),
                    lockedUntil
            );
        }

        userRepository.save(user);
    }

    // =========================================================
    // Validate account
    // =========================================================

    private void validateAccountForAuthentication(
            User user
    ) {

        if (user.getAccountStatus()
                != AccountStatus.ACTIVE) {

            log.warn(
                    "Authentication rejected because account is not active: "
                            + "userId={}, status={}",
                    user.getId(),
                    user.getAccountStatus()
            );

            throw new IllegalStateException(
                    "Your account is not active"
            );
        }

        if (!Boolean.TRUE.equals(
                user.getAccountLocked()
        )) {

            return;
        }

        LocalDateTime lockedUntil =
                user.getAccountLockedUntil();

        /*
         * If the temporary lock has expired, automatically
         * unlock the account during login or token refresh.
         */
        if (lockedUntil != null
                && !lockedUntil.isAfter(
                LocalDateTime.now()
        )) {

            resetLoginSecurityState(user);

            userRepository.save(user);

            log.info(
                    "Account unlocked automatically after lock period expired: userId={}",
                    user.getId()
            );

            return;
        }

        String message;

        if (lockedUntil == null) {

            message =
                    "Your account is locked. "
                            + "Please contact support";

        } else {

            message =
                    "Your account is temporarily locked until "
                            + lockedUntil;
        }

        log.warn(
                "Authentication rejected because account is locked: "
                        + "userId={}, lockedUntil={}",
                user.getId(),
                lockedUntil
        );

        throw new AccountLockedException(
                message,
                lockedUntil
        );
    }

    // =========================================================
    // Reset login security state
    // =========================================================

    private void resetLoginSecurityState(
            User user
    ) {

        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setAccountLockedUntil(null);
    }

    // =========================================================
    // Validate login security configuration
    // =========================================================

    private void validateLoginSecurityConfiguration() {

        if (maxFailedLoginAttempts <= 0) {

            log.error(
                    "Invalid max-failed-login-attempts configuration: value={}",
                    maxFailedLoginAttempts
            );

            throw new IllegalStateException(
                    "Maximum failed login attempts configuration is invalid"
            );
        }

        if (accountLockDurationMinutes <= 0) {

            log.error(
                    "Invalid account-lock-duration-minutes configuration: value={}",
                    accountLockDurationMinutes
            );

            throw new IllegalStateException(
                    "Account lock duration configuration is invalid"
            );
        }
    }

    // =========================================================
    // Validate password-reset request
    // =========================================================

    private void validatePasswordResetRequest(
            ResetPasswordRequest request
    ) {

        if (request.getOtp() == null
                || !request.getOtp()
                .trim()
                .matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "OTP must contain exactly 6 digits"
            );
        }

        if (request.getNewPassword() == null
                || request.getNewPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "New password is required"
            );
        }

        if (request.getConfirmPassword() == null
                || request.getConfirmPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Confirm password is required"
            );
        }

        if (!request.getNewPassword().equals(
                request.getConfirmPassword()
        )) {

            throw new IllegalArgumentException(
                    "New password and confirm password do not match"
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
                    "Email address is required"
            );
        }

        String normalizedEmail =
                email.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

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
    // Normalize phone
    // =========================================================

    private String normalizePhone(
            String phone
    ) {

        if (phone == null
                || phone.isBlank()) {

            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        return phone.trim();
    }

    // =========================================================
    // Normalize required text
    // =========================================================

    private String normalizeRequiredText(
            String value,
            String fieldName
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        return value.trim();
    }

    // =========================================================
    // Build user's full name
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
                (firstName + " " + lastName)
                        .trim();

        return fullName.isBlank()
                ? "User"
                : fullName;
    }

    // =========================================================
    // Calculate refresh-token expiry
    // =========================================================

    private LocalDateTime calculateRefreshTokenExpiry() {

        if (refreshTokenExpiration == null
                || refreshTokenExpiration <= 0) {

            log.error(
                    "Invalid refresh-token expiration configuration: value={}",
                    refreshTokenExpiration
            );

            throw new IllegalStateException(
                    "Refresh-token expiration configuration is invalid"
            );
        }

        return LocalDateTime.now()
                .plusSeconds(
                        refreshTokenExpiration / 1000
                );
    }
}