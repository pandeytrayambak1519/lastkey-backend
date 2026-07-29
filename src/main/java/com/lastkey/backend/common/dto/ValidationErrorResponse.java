package com.lastkey.backend.common.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    private ApiError error;

    private Map<String, String> fieldErrors;
}