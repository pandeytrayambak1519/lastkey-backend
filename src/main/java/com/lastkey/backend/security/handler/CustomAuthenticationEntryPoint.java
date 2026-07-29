package com.lastkey.backend.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException, ServletException {

        Map<String, Object> errorResponse =
                new LinkedHashMap<>();

        errorResponse.put(
                "success",
                false
        );

        errorResponse.put(
                "timestamp",
                LocalDateTime.now()
        );

        errorResponse.put(
                "status",
                HttpStatus.UNAUTHORIZED.value()
        );

        errorResponse.put(
                "error",
                HttpStatus.UNAUTHORIZED.getReasonPhrase()
        );

        errorResponse.put(
                "code",
                "AUTHENTICATION_REQUIRED"
        );

        errorResponse.put(
                "message",
                "Authentication is required to access this resource."
        );

        errorResponse.put(
                "path",
                request.getRequestURI()
        );

        response.setStatus(
                HttpStatus.UNAUTHORIZED.value()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}