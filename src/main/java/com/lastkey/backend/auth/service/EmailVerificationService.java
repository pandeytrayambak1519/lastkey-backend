package com.lastkey.backend.auth.service;

import com.lastkey.backend.auth.dto.request.ResendOtpRequest;
import com.lastkey.backend.auth.dto.request.VerifyEmailRequest;
import com.lastkey.backend.auth.dto.response.EmailVerificationResponse;
import com.lastkey.backend.user.entity.User;

public interface EmailVerificationService {

    void createAndSendOtp(User user);

    EmailVerificationResponse verifyEmail(
            VerifyEmailRequest request
    );

    EmailVerificationResponse resendOtp(
            ResendOtpRequest request
    );
}