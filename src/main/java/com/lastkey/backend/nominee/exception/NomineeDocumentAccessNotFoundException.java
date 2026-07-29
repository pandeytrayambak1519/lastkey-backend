package com.lastkey.backend.nominee.exception;

public class NomineeDocumentAccessNotFoundException
        extends RuntimeException {

    public NomineeDocumentAccessNotFoundException(
            String message
    ) {
        super(message);
    }
}