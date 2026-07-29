package com.lastkey.backend.user.exception;

public class EmailAlreadyVerifiedException extends RuntimeException {

    public EmailAlreadyVerifiedException(String message) {
        super(message);
    }
}