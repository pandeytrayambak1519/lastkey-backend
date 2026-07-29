package com.lastkey.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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