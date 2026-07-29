package com.lastkey.backend.emergency.exception;

public class EmergencyAccessDeniedException
        extends RuntimeException {

    public EmergencyAccessDeniedException(String message) {
        super(message);
    }
}