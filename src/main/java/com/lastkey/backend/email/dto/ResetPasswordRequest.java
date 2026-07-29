package com.lastkey.backend.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "OTP must contain exactly 6 digits"
    )
    private String otp;

    @NotBlank(message = "New password is required")
    @Size(
            min = 8,
            max = 100,
            message = "Password must contain between 8 and 100 characters"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,100}$",
            message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}