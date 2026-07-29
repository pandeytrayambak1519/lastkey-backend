package com.lastkey.backend.user.dto.response;

public class ProfileImageResponse {

    private String message;
    private String profileImageUrl;

    public ProfileImageResponse() {
    }

    public ProfileImageResponse(
            String message,
            String profileImageUrl
    ) {
        this.message = message;
        this.profileImageUrl = profileImageUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}