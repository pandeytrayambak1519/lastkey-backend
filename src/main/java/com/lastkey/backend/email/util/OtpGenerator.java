package com.lastkey.backend.email.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    private static final int OTP_LENGTH = 6;

    private static final int OTP_UPPER_BOUND = 1_000_000;

    private final SecureRandom secureRandom;

    public OtpGenerator() {
        this.secureRandom = new SecureRandom();
    }

    public String generateOtp() {

        int otpNumber =
                secureRandom.nextInt(
                        OTP_UPPER_BOUND
                );

        return String.format(
                "%0" + OTP_LENGTH + "d",
                otpNumber
        );
    }
}