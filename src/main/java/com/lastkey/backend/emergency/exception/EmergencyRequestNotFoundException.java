package com.lastkey.backend.emergency.exception;

public class EmergencyRequestNotFoundException
        extends RuntimeException {

    public EmergencyRequestNotFoundException(String message) {
        super(message);
    }
}