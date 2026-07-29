package com.lastkey.backend.user.exception;

public class PasswordSameAsCurrentException extends RuntimeException {

    public PasswordSameAsCurrentException(String message) {
        super(message);
    }
}
