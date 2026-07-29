package com.lastkey.backend.common.dto;

import com.lastkey.backend.common.enums.ErrorCode;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private LocalDateTime timestamp;

    private int status;

    private String error;

    private ErrorCode errorCode;

    private String message;

    private String path;
}