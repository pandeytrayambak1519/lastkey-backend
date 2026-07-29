package com.lastkey.backend.user.service;

import com.lastkey.backend.common.dto.MessageResponse;
import com.lastkey.backend.user.dto.request.ChangePasswordRequest;
import com.lastkey.backend.user.dto.request.DeactivateAccountRequest;
import com.lastkey.backend.user.dto.request.UpdateProfileRequest;
import com.lastkey.backend.user.dto.request.VerifyEmailOtpRequest;
import com.lastkey.backend.user.dto.response.ProfileImageResponse;
import com.lastkey.backend.user.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateCurrentUserProfile(
            UpdateProfileRequest request
    );

    MessageResponse changeCurrentUserPassword(
            ChangePasswordRequest request
    );

    ProfileImageResponse uploadProfileImage(
            MultipartFile file
    );

    MessageResponse sendEmailVerificationOtp();

    MessageResponse verifyEmailOtp(
            VerifyEmailOtpRequest request
    );
    

    
    MessageResponse deactivateCurrentUserAccount(
            DeactivateAccountRequest request
    );
}