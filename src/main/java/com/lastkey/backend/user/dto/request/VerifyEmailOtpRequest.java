package com.lastkey.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyEmailOtpRequest {

    @NotBlank(message = "OTP is required")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP must contain exactly 6 digits"
    )
    private String otp;

    public VerifyEmailOtpRequest() {
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}