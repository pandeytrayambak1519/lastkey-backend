package com.lastkey.backend.nominee.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class NomineeExceptionHandler {

    @ExceptionHandler(NomineeNotFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleNomineeNotFound(
            NomineeNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(NomineeAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>>
    handleNomineeAlreadyExists(
            NomineeAlreadyExistsException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(
            NomineeDocumentAlreadyAssignedException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleDocumentAlreadyAssigned(
            NomineeDocumentAlreadyAssignedException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(
            NomineeDocumentAccessNotFoundException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleDocumentAccessNotFound(
            NomineeDocumentAccessNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    private ResponseEntity<Map<String, Object>>
    buildResponse(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                LocalDateTime.now()
        );

        body.put(
                "status",
                status.value()
        );

        body.put(
                "error",
                status.getReasonPhrase()
        );

        body.put(
                "message",
                message
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}