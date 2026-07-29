package com.lastkey.backend.exception;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private Boolean success;

    private LocalDateTime timestamp;

    private Integer status;

    private String error;

    private ErrorCode code;

    private String message;

    private String path;

    private List<FieldValidationError>
            validationErrors;
}