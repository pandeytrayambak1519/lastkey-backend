package com.lastkey.backend.nominee.exception;

public class NomineeAlreadyExistsException
        extends RuntimeException {

    public NomineeAlreadyExistsException(String message) {
        super(message);
    }
}