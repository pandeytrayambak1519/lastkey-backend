package com.lastkey.backend.audit.filter;

import com.lastkey.backend.audit.entity.AuditLog;
import com.lastkey.backend.audit.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingFilter
        extends OncePerRequestFilter {

    private static final Set<String> EXCLUDED_PREFIXES =
            Set.of(
                    "/swagger-ui",
                    "/v3/api-docs",
                    "/favicon.ico",
                    "/actuator/health"
            );

    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime =
                System.currentTimeMillis();

        Exception requestException = null;

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } catch (IOException
                 | ServletException
                 | RuntimeException exception) {

            requestException = exception;

            throw exception;

        } finally {

            long executionTime =
                    System.currentTimeMillis()
                            - startTime;

            try {

                saveAuditLog(
                        request,
                        response,
                        requestException,
                        executionTime
                );

            } catch (Exception auditException) {

                /*
                 * Audit logging failure should never stop
                 * the main API response.
                 */
                log.error(
                        "Failed to store audit log for {} {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        auditException
                );
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String requestUri =
                request.getRequestURI();

        return EXCLUDED_PREFIXES
                .stream()
                .anyMatch(
                        requestUri::startsWith
                );
    }

    private void saveAuditLog(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception,
            long executionTime
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String actorEmail =
                getActorEmail(authentication);

        String actorType =
                getActorType(authentication);

        String requestUri =
                request.getRequestURI();

        int httpStatus =
                response.getStatus();

        boolean successful =
                exception == null
                        && httpStatus >= 200
                        && httpStatus < 400;

        AuditLog auditLog =
                AuditLog.builder()
                        .actorEmail(actorEmail)
                        .actorType(actorType)
                        .action(
                                buildAction(
                                        request.getMethod(),
                                        requestUri
                                )
                        )
                        .resourceType(
                                extractResourceType(
                                        requestUri
                                )
                        )
                        .resourceId(
                                extractResourceId(
                                        requestUri
                                )
                        )
                        .httpMethod(
                                request.getMethod()
                        )
                        .endpoint(requestUri)
                        .httpStatus(httpStatus)
                        .successful(successful)
                        .ipAddress(
                                extractClientIp(
                                        request
                                )
                        )
                        .userAgent(
                                truncate(
                                        request.getHeader(
                                                "User-Agent"
                                        ),
                                        1000
                                )
                        )
                        .executionTimeMs(
                                executionTime
                        )
                        .failureMessage(
                                exception != null
                                        ? truncate(
                                                exception.getMessage(),
                                                2000
                                        )
                                        : null
                        )
                        .build();

        auditLogService.saveAuditLog(
                auditLog
        );
    }

    /*
     * ---------------------------------------------------------
     * GET ACTOR EMAIL
     * ---------------------------------------------------------
     */

    private String getActorEmail(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(
                authentication.getName()
        )) {

            return "ANONYMOUS";
        }

        return authentication
                .getName()
                .trim()
                .toLowerCase();
    }

    /*
     * ---------------------------------------------------------
     * GET ACTOR TYPE
     * ---------------------------------------------------------
     */

    private String getActorType(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return "ANONYMOUS";
        }

        Set<String> authorities =
                authentication
                        .getAuthorities()
                        .stream()
                        .map(
                                GrantedAuthority::getAuthority
                        )
                        .collect(
                                Collectors.toSet()
                        );

        if (authorities.contains("ROLE_ADMIN")) {
            return "ADMIN";
        }

        if (authorities.contains("ROLE_USER")) {
            return "USER";
        }

        return "AUTHENTICATED_USER";
    }

    /*
     * ---------------------------------------------------------
     * BUILD ACTION NAME
     * ---------------------------------------------------------
     */

    private String buildAction(
            String method,
            String requestUri
    ) {

        String normalizedPath =
                requestUri
                        .replaceAll(
                                "[^a-zA-Z0-9]+",
                                "_"
                        )
                        .replaceAll(
                                "^_+|_+$",
                                ""
                        )
                        .toUpperCase();

        return method.toUpperCase()
                + "_"
                + normalizedPath;
    }

    /*
     * ---------------------------------------------------------
     * EXTRACT RESOURCE TYPE
     * ---------------------------------------------------------
     */

    private String extractResourceType(
            String requestUri
    ) {

        String[] pathParts =
                Arrays.stream(
                                requestUri.split("/")
                        )
                        .filter(
                                part -> !part.isBlank()
                        )
                        .toArray(String[]::new);

        /*
         * Expected API path:
         * /api/v1/documents/{id}
         *
         * Resource is generally the third path section.
         */
        if (pathParts.length >= 3
                && "api".equalsIgnoreCase(
                pathParts[0]
        )) {

            return normalizeResourceType(
                    pathParts[2]
            );
        }

        if (pathParts.length > 0) {

            return normalizeResourceType(
                    pathParts[0]
            );
        }

        return "UNKNOWN";
    }

    /*
     * ---------------------------------------------------------
     * EXTRACT RESOURCE ID
     * ---------------------------------------------------------
     */

    private String extractResourceId(
            String requestUri
    ) {

        String[] pathParts =
                Arrays.stream(
                                requestUri.split("/")
                        )
                        .filter(
                                part -> !part.isBlank()
                        )
                        .toArray(String[]::new);

        /*
         * Example:
         * /api/v1/documents/{documentId}
         *
         * Index:
         * 0 = api
         * 1 = v1
         * 2 = documents
         * 3 = documentId
         */
        if (pathParts.length >= 4
                && "api".equalsIgnoreCase(
                pathParts[0]
        )) {

            String possibleId =
                    pathParts[3];

            if (isIdentifier(possibleId)) {
                return possibleId;
            }
        }

        return null;
    }

    private boolean isIdentifier(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return false;
        }

        return value.matches(
                "^[0-9a-fA-F-]{16,36}$"
        ) || value.matches(
                "^\\d+$"
        );
    }

    private String normalizeResourceType(
            String resource
    ) {

        if (resource == null
                || resource.isBlank()) {

            return "UNKNOWN";
        }

        String normalized =
                resource
                        .replaceAll(
                                "[^a-zA-Z0-9]+",
                                "_"
                        )
                        .replaceAll(
                                "^_+|_+$",
                                ""
                        )
                        .toUpperCase();

        /*
         * Convert common plural resource names
         * into readable singular names.
         */
        return switch (normalized) {

            case "DOCUMENTS" -> "DOCUMENT";
            case "CATEGORIES" -> "CATEGORY";
            case "NOMINEES" -> "NOMINEE";
            case "EMERGENCIES" -> "EMERGENCY";
            case "USERS" -> "USER";
            case "NOTIFICATIONS" -> "NOTIFICATION";

            default -> normalized;
        };
    }

    /*
     * ---------------------------------------------------------
     * EXTRACT CLIENT IP
     * ---------------------------------------------------------
     */

    private String extractClientIp(
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

        return request.getRemoteAddr();
    }

    /*
     * ---------------------------------------------------------
     * SAFE TEXT LENGTH
     * ---------------------------------------------------------
     */

    private String truncate(
            String value,
            int maximumLength
    ) {

        if (value == null) {
            return null;
        }

        String trimmedValue =
                value.trim();

        if (trimmedValue.length()
                <= maximumLength) {

            return trimmedValue;
        }

        return trimmedValue.substring(
                0,
                maximumLength
        );
    }
}