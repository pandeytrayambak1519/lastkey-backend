package com.lastkey.backend.nominee.exception;

public class NomineeDocumentAlreadyAssignedException
        extends RuntimeException {

    public NomineeDocumentAlreadyAssignedException(
            String message
    ) {
        super(message);
    }
}