package com.lastkey.backend.exception;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidationError {

    private String field;

    private String message;

    private Object rejectedValue;
}