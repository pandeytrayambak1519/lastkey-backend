package com.lastkey.backend.user.exception;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }
}