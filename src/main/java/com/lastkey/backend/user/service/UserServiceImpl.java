package com.lastkey.backend.user.service;

import com.lastkey.backend.common.dto.MessageResponse;
import com.lastkey.backend.common.enums.AccountStatus;
import com.lastkey.backend.email.service.EmailService;
import com.lastkey.backend.user.dto.request.ChangePasswordRequest;
import com.lastkey.backend.user.dto.request.DeactivateAccountRequest;
import com.lastkey.backend.user.dto.request.UpdateProfileRequest;
import com.lastkey.backend.user.dto.request.VerifyEmailOtpRequest;
import com.lastkey.backend.user.dto.response.ProfileImageResponse;
import com.lastkey.backend.user.dto.response.UserProfileResponse;
import com.lastkey.backend.user.entity.EmailVerificationOtp;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.exception.EmailAlreadyVerifiedException;
import com.lastkey.backend.user.exception.FileStorageException;
import com.lastkey.backend.user.exception.InvalidCurrentPasswordException;
import com.lastkey.backend.user.exception.InvalidFileException;
import com.lastkey.backend.user.exception.InvalidOtpException;
import com.lastkey.backend.user.exception.OtpExpiredException;
import com.lastkey.backend.user.exception.PasswordMismatchException;
import com.lastkey.backend.user.exception.PasswordSameAsCurrentException;
import com.lastkey.backend.user.exception.PhoneAlreadyExistsException;
import com.lastkey.backend.user.exception.UserNotFoundException;
import com.lastkey.backend.user.mapper.UserMapper;
import com.lastkey.backend.user.repository.EmailVerificationOtpRepository;
import com.lastkey.backend.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final long MAX_PROFILE_IMAGE_SIZE =
            5L * 1024L * 1024L;

    private static final int OTP_EXPIRATION_MINUTES = 10;

    private static final int MAX_OTP_ATTEMPTS = 5;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationOtpRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom;
    private final Path profileImagesDirectory;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            EmailVerificationOtpRepository otpRepository,
            EmailService emailService,
            @Value("${app.upload.profile-images-dir}")
            String profileImagesDirectory
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();

        this.profileImagesDirectory =
                Paths.get(profileImagesDirectory)
                        .toAbsolutePath()
                        .normalize();
    }

    @Override
    @Transactional
    public MessageResponse deactivateCurrentUserAccount(
            DeactivateAccountRequest request
    ) {

        User user = getAuthenticatedUser();

        if (user.getAccountStatus() == AccountStatus.DEACTIVATED) {
            throw new IllegalStateException(
                    "Account is already deactivated"
            );
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPassword()
                );

        if (!passwordMatches) {
            throw new InvalidCurrentPasswordException(
                    "Current password is incorrect"
            );
        }

        user.setAccountStatus(AccountStatus.DEACTIVATED);
        user.setDeactivatedAt(LocalDateTime.now());

        userRepository.save(user);

        return new MessageResponse(
                "Account deactivated successfully"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {

        User user = getAuthenticatedUser();

        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(
            UpdateProfileRequest request
    ) {

        User user = getAuthenticatedUser();

        String normalizedPhone =
                request.getPhone().trim();

        boolean phoneAlreadyUsed =
                userRepository.existsByPhoneAndIdNot(
                        normalizedPhone,
                        user.getId()
                );

        if (phoneAlreadyUsed) {
            throw new PhoneAlreadyExistsException(
                    "Phone number is already registered"
            );
        }

        user.setFirstName(
                request.getFirstName().trim()
        );

        user.setLastName(
                request.getLastName().trim()
        );

        user.setPhone(normalizedPhone);

        User updatedUser =
                userRepository.save(user);

        return userMapper.toProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public MessageResponse changeCurrentUserPassword(
            ChangePasswordRequest request
    ) {

        User user = getAuthenticatedUser();

        boolean currentPasswordCorrect =
                passwordEncoder.matches(
                        request.getCurrentPassword(),
                        user.getPassword()
                );

        if (!currentPasswordCorrect) {
            throw new InvalidCurrentPasswordException(
                    "Current password is incorrect"
            );
        }

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new PasswordMismatchException(
                    "New password and confirm password do not match"
            );
        }

        boolean sameAsCurrentPassword =
                passwordEncoder.matches(
                        request.getNewPassword(),
                        user.getPassword()
                );

        if (sameAsCurrentPassword) {
            throw new PasswordSameAsCurrentException(
                    "New password must be different from current password"
            );
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        return new MessageResponse(
                "Password changed successfully"
        );
    }

    @Override
    @Transactional
    public ProfileImageResponse uploadProfileImage(
            MultipartFile file
    ) {

        User user = getAuthenticatedUser();

        validateProfileImage(file);

        try {
            Files.createDirectories(
                    profileImagesDirectory
            );

            String extension =
                    getFileExtension(
                            file.getOriginalFilename()
                    );

            String storedFileName =
                    user.getId()
                            + "-"
                            + UUID.randomUUID()
                            + extension;

            Path targetLocation =
                    profileImagesDirectory
                            .resolve(storedFileName)
                            .normalize();

            if (!targetLocation.startsWith(
                    profileImagesDirectory
            )) {
                throw new InvalidFileException(
                        "Invalid profile image path"
                );
            }

            deleteExistingProfileImage(
                    user.getProfileImageUrl()
            );

            Files.copy(
                    file.getInputStream(),
                    targetLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String profileImageUrl =
                    "/uploads/profile-images/"
                            + storedFileName;

            user.setProfileImageUrl(profileImageUrl);

            userRepository.save(user);

            return new ProfileImageResponse(
                    "Profile image uploaded successfully",
                    profileImageUrl
            );

        } catch (InvalidFileException exception) {
            throw exception;

        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not store profile image",
                    exception
            );
        }
    }

    @Override
    @Transactional
    public MessageResponse sendEmailVerificationOtp() {

        User user = getAuthenticatedUser();

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailAlreadyVerifiedException(
                    "Email address is already verified"
            );
        }

        String otp = generateSixDigitOtp();

        /*
         * Reuse the existing OTP record for this user.
         *
         * Because user_id is unique in the OTP table, creating a new row
         * every time can cause a duplicate-key conflict. Updating the
         * existing record avoids that problem.
         */
        EmailVerificationOtp verificationOtp =
                otpRepository.findByUser(user)
                        .orElseGet(EmailVerificationOtp::new);

        verificationOtp.setUser(user);

        verificationOtp.setOtpHash(
                passwordEncoder.encode(otp)
        );

        verificationOtp.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(OTP_EXPIRATION_MINUTES)
        );

        verificationOtp.setAttemptCount(0);

        otpRepository.save(verificationOtp);

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

        if (fullName.isBlank()) {
            fullName = "User";
        }

        emailService.sendOtpEmail(
                user.getEmail(),
                fullName,
                otp,
                OTP_EXPIRATION_MINUTES
        );

        return new MessageResponse(
                "Verification OTP sent successfully"
        );
    }

    @Override
    @Transactional
    public MessageResponse verifyEmailOtp(
            VerifyEmailOtpRequest request
    ) {

        User user = getAuthenticatedUser();

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailAlreadyVerifiedException(
                    "Email address is already verified"
            );
        }

        EmailVerificationOtp verificationOtp =
                otpRepository.findByUser(user)
                        .orElseThrow(
                                () -> new InvalidOtpException(
                                        "No active verification OTP found"
                                )
                        );

        if (verificationOtp.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            otpRepository.delete(verificationOtp);

            throw new OtpExpiredException(
                    "Verification OTP has expired"
            );
        }

        int attemptCount =
                verificationOtp.getAttemptCount() == null
                        ? 0
                        : verificationOtp.getAttemptCount();

        if (attemptCount >= MAX_OTP_ATTEMPTS) {

            otpRepository.delete(verificationOtp);

            throw new InvalidOtpException(
                    "Maximum OTP attempts exceeded. Request a new OTP"
            );
        }

        boolean otpMatches =
                passwordEncoder.matches(
                        request.getOtp(),
                        verificationOtp.getOtpHash()
                );

        if (!otpMatches) {

            int updatedAttempts =
                    attemptCount + 1;

            verificationOtp.setAttemptCount(
                    updatedAttempts
            );

            otpRepository.save(verificationOtp);

            int remainingAttempts =
                    MAX_OTP_ATTEMPTS - updatedAttempts;

            throw new InvalidOtpException(
                    "Invalid verification OTP. Remaining attempts: "
                            + remainingAttempts
            );
        }

        user.setEmailVerified(true);

        userRepository.save(user);

        otpRepository.delete(verificationOtp);

        return new MessageResponse(
                "Email verified successfully"
        );
    }

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new UserNotFoundException(
                    "Authenticated user information is not available"
            );
        }

        String email =
                authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new UserNotFoundException(
                                "User not found with email: "
                                        + email
                        )
                );
    }

    private String generateSixDigitOtp() {

        int otpNumber =
                100000
                        + secureRandom.nextInt(
                        900000
                );

        return String.valueOf(
                otpNumber
        );
    }

    private void validateProfileImage(
            MultipartFile file
    ) {

        if (file == null
                || file.isEmpty()) {

            throw new InvalidFileException(
                    "Profile image is required"
            );
        }

        if (file.getSize()
                > MAX_PROFILE_IMAGE_SIZE) {

            throw new InvalidFileException(
                    "Profile image size must not exceed 5 MB"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(
                contentType.toLowerCase(
                        Locale.ROOT
                )
        )) {

            throw new InvalidFileException(
                    "Only JPG, PNG and WEBP images are allowed"
            );
        }

        String extension =
                getFileExtension(
                        file.getOriginalFilename()
                );

        Set<String> allowedExtensions =
                Set.of(
                        ".jpg",
                        ".jpeg",
                        ".png",
                        ".webp"
                );

        if (!allowedExtensions.contains(
                extension
        )) {

            throw new InvalidFileException(
                    "Invalid profile image extension"
            );
        }
    }

    private String getFileExtension(
            String fileName
    ) {

        if (fileName == null
                || fileName.isBlank()
                || !fileName.contains(".")) {

            throw new InvalidFileException(
                    "Profile image must have a valid extension"
            );
        }

        return fileName.substring(
                fileName.lastIndexOf(".")
        ).toLowerCase(
                Locale.ROOT
        );
    }

    private void deleteExistingProfileImage(
            String existingImageUrl
    ) {

        if (existingImageUrl == null
                || existingImageUrl.isBlank()) {

            return;
        }

        String prefix =
                "/uploads/profile-images/";

        if (!existingImageUrl.startsWith(
                prefix
        )) {
            return;
        }

        String existingFileName =
                existingImageUrl.substring(
                        prefix.length()
                );

        Path existingFilePath =
                profileImagesDirectory
                        .resolve(existingFileName)
                        .normalize();

        if (!existingFilePath.startsWith(
                profileImagesDirectory
        )) {
            return;
        }

        try {
            Files.deleteIfExists(
                    existingFilePath
            );

        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not replace existing profile image",
                    exception
            );
        }
    }
}