package com.lastkey.backend.nominee.exception;

public class NomineeNotFoundException
        extends RuntimeException {

    public NomineeNotFoundException(String message) {
        super(message);
    }
}