package com.lastkey.backend.nominee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NomineeVerificationRequest {

    @NotBlank(message = "OTP is required")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "OTP must contain exactly 6 digits"
    )
    private String otp;
}