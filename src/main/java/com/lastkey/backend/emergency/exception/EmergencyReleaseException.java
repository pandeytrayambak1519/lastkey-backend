package com.lastkey.backend.emergency.exception;

public class EmergencyReleaseException
        extends RuntimeException {

    public EmergencyReleaseException(String message) {
        super(message);
    }

    public EmergencyReleaseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}