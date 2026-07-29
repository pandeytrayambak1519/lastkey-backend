package com.lastkey.backend.user.exception;

public class OtpExpiredException extends RuntimeException {

    public OtpExpiredException(String message) {
        super(message);
    }
}