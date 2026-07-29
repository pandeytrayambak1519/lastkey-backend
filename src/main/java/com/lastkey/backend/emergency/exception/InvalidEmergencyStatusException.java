package com.lastkey.backend.emergency.exception;

public class InvalidEmergencyStatusException
        extends RuntimeException {

    public InvalidEmergencyStatusException(String message) {
        super(message);
    }
}