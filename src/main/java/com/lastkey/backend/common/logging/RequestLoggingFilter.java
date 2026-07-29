package com.lastkey.backend.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter
        extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER =
            "X-Request-Id";

    private static final String REQUEST_ID_MDC_KEY =
            "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime =
                System.currentTimeMillis();

        String requestId =
                resolveRequestId(request);

        MDC.put(
                REQUEST_ID_MDC_KEY,
                requestId
        );

        response.setHeader(
                REQUEST_ID_HEADER,
                requestId
        );

        try {

            log.info(
                    "Incoming request: method={}, path={}, query={}, clientIp={}, userAgent={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    sanitizeQueryString(
                            request.getQueryString()
                    ),
                    resolveClientIp(request),
                    request.getHeader(
                            "User-Agent"
                    )
            );

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            long duration =
                    System.currentTimeMillis()
                            - startTime;

            log.info(
                    "Completed request: method={}, path={}, status={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
            );

            MDC.remove(
                    REQUEST_ID_MDC_KEY
            );
        }
    }

    private String resolveRequestId(
            HttpServletRequest request
    ) {

        return Optional
                .ofNullable(
                        request.getHeader(
                                REQUEST_ID_HEADER
                        )
                )
                .filter(
                        value -> !value.isBlank()
                )
                .map(
                        value -> value
                                .trim()
                                .substring(
                                        0,
                                        Math.min(
                                                value.trim().length(),
                                                100
                                        )
                                )
                )
                .orElseGet(
                        () -> UUID
                                .randomUUID()
                                .toString()
                );
    }

    private String resolveClientIp(
            HttpServletRequest request
    ) {

        String forwardedFor =
                request.getHeader(
                        "X-Forwarded-For"
                );

        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        String realIp =
                request.getHeader(
                        "X-Real-IP"
                );

        if (realIp != null
                && !realIp.isBlank()) {

            return realIp.trim();
        }

        return request
                .getRemoteAddr();
    }

    private String sanitizeQueryString(
            String queryString
    ) {

        if (queryString == null
                || queryString.isBlank()) {

            return "-";
        }

        return queryString
                .replaceAll(
                        "(?i)(token|accessToken|refreshToken|password|otp|secret)=([^&]*)",
                        "$1=***"
                );
    }
}