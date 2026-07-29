package com.lastkey.backend.auth.service;

import com.lastkey.backend.auth.dto.request.LoginRequest;
import com.lastkey.backend.auth.dto.request.RegisterRequest;
import com.lastkey.backend.auth.dto.response.LoginResponse;
import com.lastkey.backend.auth.dto.response.RegisterResponse;
import com.lastkey.backend.email.dto.OtpRequest;
import com.lastkey.backend.email.dto.ResetPasswordRequest;

public interface AuthService {

    RegisterResponse register(
            RegisterRequest request
    );

    LoginResponse login(
            LoginRequest request
    );

    LoginResponse refreshToken(
            String refreshToken
    );

    void logout(
            String refreshToken
    );

    void forgotPassword(
            OtpRequest request
    );

    void resetPassword(
            ResetPasswordRequest request
    );
}