package com.lastkey.backend.security.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityController {

    /*
     * Security Center overview
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSecurityOverview(
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();

        /*
         * Fields required by the premium Security Center frontend
         */
        response.put("securityScore", 82);
        response.put("securityLabel", "Strong");
        response.put("activeSessionCount", 1);
        response.put("failedLoginCount", 0);
        response.put("verifiedNomineeCount", 1);

        List<Map<String, Object>> recommendations = new ArrayList<>();

        Map<String, Object> recommendation1 = new HashMap<>();
        recommendation1.put("id", "enable-two-factor-authentication");
        recommendation1.put("title", "Enable two-factor authentication");
        recommendation1.put(
                "description",
                "Add an additional layer of protection to your LastKey account."
        );
        recommendation1.put("completed", false);
        recommendation1.put("priority", "HIGH");
        recommendations.add(recommendation1);

        Map<String, Object> recommendation2 = new HashMap<>();
        recommendation2.put("id", "review-active-sessions");
        recommendation2.put("title", "Review active sessions");
        recommendation2.put(
                "description",
                "Verify that every signed-in device belongs to you."
        );
        recommendation2.put("completed", true);
        recommendation2.put("priority", "MEDIUM");
        recommendations.add(recommendation2);

        response.put("recommendations", recommendations);

        /*
         * Existing fields retained for backward compatibility
         */
        response.put("accountSecure", true);
        response.put("emailVerified", true);
        response.put("twoFactorEnabled", false);
        response.put("activeSessions", 1);
        response.put("recentLoginAttempts", 0);
        response.put("lastPasswordChange", null);
        response.put("lastLoginAt", LocalDateTime.now());
        response.put("userEmail", authentication.getName());

        return ResponseEntity.ok(response);
    }

    /*
     * Paginated login activity
     */
    @GetMapping("/login-activity")
    public ResponseEntity<Map<String, Object>> getLoginActivity(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(
                    defaultValue = "createdAt,desc"
            ) String sort
    ) {
        List<Map<String, Object>> activities = new ArrayList<>();

        Map<String, Object> currentLogin = new HashMap<>();
        currentLogin.put("id", UUID.randomUUID().toString());
        currentLogin.put("deviceName", "Windows Desktop");
        currentLogin.put("deviceType", "DESKTOP");
        currentLogin.put("browser", "Google Chrome");
        currentLogin.put("operatingSystem", "Windows 10");
        currentLogin.put("location", "Ghaziabad, India");
        currentLogin.put("ipAddress", "127.0.0.1");
        currentLogin.put("status", "SUCCESS");
        currentLogin.put("failureReason", null);
        currentLogin.put("createdAt", LocalDateTime.now());
        currentLogin.put("userEmail", authentication.getName());
        activities.add(currentLogin);

        Map<String, Object> previousLogin = new HashMap<>();
        previousLogin.put("id", UUID.randomUUID().toString());
        previousLogin.put("deviceName", "Android Phone");
        previousLogin.put("deviceType", "MOBILE");
        previousLogin.put("browser", "Google Chrome");
        previousLogin.put("operatingSystem", "Android");
        previousLogin.put("location", "Ghaziabad, India");
        previousLogin.put("ipAddress", "192.168.1.5");
        previousLogin.put("status", "SUCCESS");
        previousLogin.put("failureReason", null);
        previousLogin.put(
                "createdAt",
                LocalDateTime.now().minusDays(1)
        );
        previousLogin.put("userEmail", authentication.getName());
        activities.add(previousLogin);

        /*
         * Spring Page-like response expected by frontend pagination.
         */
        Map<String, Object> response = new HashMap<>();
        response.put("content", activities);
        response.put("page", page);
        response.put("number", page);
        response.put("size", size);
        response.put("sort", sort);
        response.put("totalElements", activities.size());
        response.put("totalPages", 1);
        response.put("first", page == 0);
        response.put("last", true);
        response.put("empty", activities.isEmpty());

        return ResponseEntity.ok(response);
    }

    /*
     * Active signed-in sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getActiveSessions(
            Authentication authentication
    ) {
        List<Map<String, Object>> sessions = new ArrayList<>();

        Map<String, Object> currentSession = new HashMap<>();
        currentSession.put(
                "id",
                "current-session"
        );
        currentSession.put(
                "deviceName",
                "Windows Desktop"
        );
        currentSession.put(
                "deviceType",
                "DESKTOP"
        );
        currentSession.put(
                "browser",
                "Google Chrome"
        );
        currentSession.put(
                "operatingSystem",
                "Windows 10"
        );
        currentSession.put(
                "location",
                "Ghaziabad, India"
        );
        currentSession.put(
                "ipAddress",
                "127.0.0.1"
        );
        currentSession.put(
                "current",
                true
        );
        currentSession.put(
                "createdAt",
                LocalDateTime.now().minusHours(1)
        );
        currentSession.put(
                "lastActivityAt",
                LocalDateTime.now()
        );
        currentSession.put(
                "userEmail",
                authentication.getName()
        );

        sessions.add(currentSession);

        return ResponseEntity.ok(sessions);
    }

    /*
     * Revoke one particular session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        /*
         * Later, delete the session from the database using:
         * sessionId + authentication.getName()
         */

        return ResponseEntity.noContent().build();
    }

    /*
     * Revoke every session except the current one
     */
    @DeleteMapping("/sessions/others")
    public ResponseEntity<Void> revokeOtherSessions(
            Authentication authentication
    ) {
        /*
         * Later, delete all non-current sessions belonging to:
         * authentication.getName()
         */

        return ResponseEntity.noContent().build();
    }
}