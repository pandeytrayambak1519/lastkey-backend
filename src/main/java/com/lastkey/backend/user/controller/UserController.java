package com.lastkey.backend.user.controller;

import com.lastkey.backend.common.dto.MessageResponse;
import com.lastkey.backend.user.dto.request.ChangePasswordRequest;
import com.lastkey.backend.user.dto.request.DeactivateAccountRequest;
import com.lastkey.backend.user.dto.request.UpdateProfileRequest;
import com.lastkey.backend.user.dto.request.VerifyEmailOtpRequest;
import com.lastkey.backend.user.dto.response.ProfileImageResponse;
import com.lastkey.backend.user.dto.response.UserProfileResponse;
import com.lastkey.backend.user.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {

        UserProfileResponse response =
                userService.getCurrentUserProfile();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        UserProfileResponse response =
                userService.updateCurrentUserProfile(request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changeCurrentUserPassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        MessageResponse response =
                userService.changeCurrentUserPassword(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProfileImageResponse> uploadProfileImage(
            @RequestPart("file") MultipartFile file
    ) {

        ProfileImageResponse response =
                userService.uploadProfileImage(file);

        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/me/email-verification/send")
    public ResponseEntity<MessageResponse> sendEmailVerificationOtp() {

        return ResponseEntity.ok(
                userService.sendEmailVerificationOtp()
        );
    }
    
    @PostMapping("/me/email-verification/verify")
    public ResponseEntity<MessageResponse> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request
    ) {

        return ResponseEntity.ok(
                userService.verifyEmailOtp(request)
        );
    }
    
    @PostMapping("/me/deactivate")
    public ResponseEntity<MessageResponse> deactivateCurrentUserAccount(
            @Valid @RequestBody DeactivateAccountRequest request
    ) {

        MessageResponse response =
                userService.deactivateCurrentUserAccount(request);

        return ResponseEntity.ok(response);
    }
}