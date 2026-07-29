package com.lastkey.backend.auth.dto.response;

public class EmailVerificationResponse {

    private String message;
    private String email;
    private Boolean emailVerified;

    public EmailVerificationResponse() {
    }

    public EmailVerificationResponse(
            String message,
            String email,
            Boolean emailVerified
    ) {
        this.message = message;
        this.email = email;
        this.emailVerified = emailVerified;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}