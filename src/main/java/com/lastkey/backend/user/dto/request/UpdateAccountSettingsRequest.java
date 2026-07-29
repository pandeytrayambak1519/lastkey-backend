package com.lastkey.backend.user.dto.request;

public record UpdateAccountSettingsRequest(

        Boolean emailNotifications,

        Boolean securityAlerts,

        Boolean documentReminders,

        Boolean emergencyNotifications,

        Boolean profileVisible,

        Boolean activityVisible,

        Integer emergencyDelayDays,

        Boolean nomineeVerificationRequired,

        String language,

        String timezone

) {
}