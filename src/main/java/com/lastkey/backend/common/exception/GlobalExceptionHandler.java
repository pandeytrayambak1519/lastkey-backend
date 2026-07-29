package com.lastkey.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * =========================================================
     * RESOURCE NOT FOUND
     * =========================================================
     */

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                resolveMessage(
                        exception,
                        "Requested resource was not found."
                ),
                request,
                null
        );
    }

    /*
     * Handles invalid URLs such as:
     * GET /api/v1/unknown-endpoint
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "ENDPOINT_NOT_FOUND",
                "The requested API endpoint was not found.",
                request,
                null
        );
    }

    /*
     * =========================================================
     * REQUEST BODY VALIDATION
     * =========================================================
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        List<Map<String, Object>> validationErrors =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::createFieldValidationError)
                        .toList();

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed.",
                request,
                validationErrors
        );
    }

    /*
     * Handles validation errors on:
     * @RequestParam
     * @PathVariable
     * Controller method parameters
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>>
    handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        List<Map<String, Object>> validationErrors =
                exception
                        .getConstraintViolations()
                        .stream()
                        .map(this::createConstraintValidationError)
                        .toList();

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed.",
                request,
                validationErrors
        );
    }

    /*
     * =========================================================
     * INVALID REQUESTS
     * =========================================================
     */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
    handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                resolveMessage(
                        exception,
                        "The supplied request is invalid."
                ),
                request,
                null
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>>
    handleIllegalStateException(
            IllegalStateException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_OPERATION",
                resolveMessage(
                        exception,
                        "The requested operation cannot be completed."
                ),
                request,
                null
        );
    }

    /*
     * Example:
     * UUID expected but user sends "abc".
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>>
    handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {

        String expectedType =
                exception.getRequiredType() != null
                        ? exception
                        .getRequiredType()
                        .getSimpleName()
                        : "valid type";

        String message =
                "Invalid value for parameter '"
                        + exception.getName()
                        + "'. Expected "
                        + expectedType
                        + ".";

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER_TYPE",
                message,
                request,
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>>
    handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {

        String message =
                "Required request parameter '"
                        + exception.getParameterName()
                        + "' is missing.";

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUEST_PARAMETER",
                message,
                request,
                null
        );
    }

    /*
     * =========================================================
     * HTTP METHOD AND CONTENT TYPE
     * =========================================================
     */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>>
    handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {

        String message =
                "HTTP method '"
                        + exception.getMethod()
                        + "' is not supported for this endpoint.";

        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                message,
                request,
                null
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>>
    handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {

        String message =
                exception.getContentType() != null
                        ? "Content type '"
                        + exception.getContentType()
                        + "' is not supported."
                        : "The provided content type is not supported.";

        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                message,
                request,
                null
        );
    }

    /*
     * =========================================================
     * SECURITY AND AUTHENTICATION
     * =========================================================
     */

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>>
    handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this operation.",
                request,
                null
        );
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            AuthenticationCredentialsNotFoundException.class,
            InsufficientAuthenticationException.class,
            AuthenticationServiceException.class
    })
    public ResponseEntity<Map<String, Object>>
    handleAuthenticationException(
            RuntimeException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                "Authentication failed. Please provide valid credentials.",
                request,
                null
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>>
    handleDisabledException(
            DisabledException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED",
                "Your account is disabled.",
                request,
                null
        );
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>>
    handleLockedException(
            LockedException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_LOCKED",
                "Your account is temporarily locked.",
                request,
                null
        );
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>>
    handleSecurityException(
            SecurityException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "SECURITY_ERROR",
                resolveMessage(
                        exception,
                        "Access to this resource is denied."
                ),
                request,
                null
        );
    }

    /*
     * =========================================================
     * FILE UPLOAD ERRORS
     * =========================================================
     */

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>>
    handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "The uploaded file exceeds the maximum allowed size.",
                request,
                null
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>>
    handleMultipartException(
            MultipartException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE_REQUEST",
                "The file upload request is invalid.",
                request,
                null
        );
    }

    /*
     * =========================================================
     * DATABASE ERRORS
     * =========================================================
     */

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>>
    handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {

        log.error(
                "Database constraint violation at path: {}",
                request.getRequestURI(),
                exception
        );

        String message =
                "The submitted data conflicts with an existing record.";

        Throwable rootCause = exception;

        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String rootMessage = rootCause.getMessage();

        if (rootMessage != null) {

            String normalizedMessage =
                    rootMessage.toLowerCase();

            if (normalizedMessage.contains(
                    "uk_nominee_owner_email"
            )) {

                message =
                        "Another nominee already uses this email.";

            } else if (normalizedMessage.contains(
                    "uk_active_nominee_owner_email"
            )) {

                message =
                        "Another active nominee already uses this email.";

            } else if (normalizedMessage.contains("email")) {

                message =
                        "An existing record already uses this email.";

            } else if (normalizedMessage.contains("phone")) {

                message =
                        "An existing record already uses this phone number.";
            }
        }

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                message,
                request,
                null
        );
    }

    /*
     * =========================================================
     * UNEXPECTED ERROR
     * =========================================================
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {

        log.error(
                "Unexpected error occurred at path: {}",
                request.getRequestURI(),
                exception
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal server error occurred.",
                request,
                null
        );
    }

    /*
     * =========================================================
     * RESPONSE BUILDER
     * =========================================================
     */

    private ResponseEntity<Map<String, Object>>
    buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request,
            List<Map<String, Object>> validationErrors
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "success",
                false
        );

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                status.value()
        );

        response.put(
                "error",
                status.getReasonPhrase()
        );

        response.put(
                "code",
                errorCode
        );

        response.put(
                "message",
                message
        );

        response.put(
                "path",
                request != null
                        ? request.getRequestURI()
                        : null
        );

        if (validationErrors != null
                && !validationErrors.isEmpty()) {

            response.put(
                    "validationErrors",
                    validationErrors
            );
        }

        return ResponseEntity
                .status(status)
                .body(response);
    }

    /*
     * =========================================================
     * VALIDATION ERROR MAPPERS
     * =========================================================
     */

    private Map<String, Object>
    createFieldValidationError(
            FieldError fieldError
    ) {

        Map<String, Object> error =
                new LinkedHashMap<>();

        error.put(
                "field",
                fieldError.getField()
        );

        error.put(
                "message",
                fieldError.getDefaultMessage()
        );

        if (!isSensitiveField(
                fieldError.getField()
        )) {

            error.put(
                    "rejectedValue",
                    fieldError.getRejectedValue()
            );
        }

        return error;
    }

    private Map<String, Object>
    createConstraintValidationError(
            ConstraintViolation<?> violation
    ) {

        Map<String, Object> error =
                new LinkedHashMap<>();

        String field =
                violation.getPropertyPath() != null
                        ? violation
                        .getPropertyPath()
                        .toString()
                        : null;

        error.put(
                "field",
                field
        );

        error.put(
                "message",
                violation.getMessage()
        );

        if (!isSensitiveField(field)) {

            error.put(
                    "rejectedValue",
                    violation.getInvalidValue()
            );
        }

        return error;
    }

    /*
     * =========================================================
     * HELPER METHODS
     * =========================================================
     */

    private String resolveMessage(
            Exception exception,
            String fallbackMessage
    ) {

        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return fallbackMessage;
        }

        return exception
                .getMessage()
                .trim();
    }

    private boolean isSensitiveField(
            String fieldName
    ) {

        if (fieldName == null) {
            return false;
        }

        String normalizedField =
                fieldName.toLowerCase();

        return normalizedField.contains("password")
                || normalizedField.contains("token")
                || normalizedField.contains("secret")
                || normalizedField.contains("key")
                || normalizedField.contains("otp")
                || normalizedField.contains("authorization");
    }
}