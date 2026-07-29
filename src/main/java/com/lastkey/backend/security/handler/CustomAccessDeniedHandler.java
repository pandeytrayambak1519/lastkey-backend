package com.lastkey.backend.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastkey.backend.exception.ApiErrorResponse;
import com.lastkey.backend.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException, ServletException {

        ApiErrorResponse errorResponse =
                ApiErrorResponse.builder()
                        .success(false)
                        .timestamp(
                                LocalDateTime.now()
                        )
                        .status(
                                HttpStatus.FORBIDDEN.value()
                        )
                        .error(
                                HttpStatus.FORBIDDEN
                                        .getReasonPhrase()
                        )
                        .code(ErrorCode.ACCESS_DENIED)
                        .message(
                                "You do not have permission to access this resource."
                        )
                        .path(request.getRequestURI())
                        .validationErrors(null)
                        .build();

        response.setStatus(
                HttpStatus.FORBIDDEN.value()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}