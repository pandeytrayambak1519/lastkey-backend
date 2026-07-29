package com.lastkey.backend.email.dto;

import com.lastkey.backend.email.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class OtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /*
     * Required while sending OTP.
     * Optional while verifying OTP.
     */
    private String userName;

    /*
     * Required only while verifying OTP.
     */
    @Pattern(
            regexp = "^\\d{6}$",
            message = "OTP must contain exactly 6 digits"
    )
    private String otp;

    @NotNull(message = "OTP purpose is required")
    private OtpPurpose purpose;
}