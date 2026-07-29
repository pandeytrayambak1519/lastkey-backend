package com.lastkey.backend.emergency.exception;

public class ActiveEmergencyRequestAlreadyExistsException
        extends RuntimeException {

    public ActiveEmergencyRequestAlreadyExistsException(
            String message
    ) {
        super(message);
    }
}
