package com.lastkey.backend.user.exception;

public class PhoneAlreadyExistsException extends RuntimeException {

    public PhoneAlreadyExistsException(String message) {
        super(message);
    }
}