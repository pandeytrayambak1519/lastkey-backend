package com.lastkey.backend.user.service.impl;

import com.lastkey.backend.user.dto.request.UpdateAccountSettingsRequest;
import com.lastkey.backend.user.dto.response.AccountSettingsResponse;
import com.lastkey.backend.user.service.UserSettingsService;

import org.springframework.stereotype.Service;

@Service
public class UserSettingsServiceImpl
        implements UserSettingsService {

    private AccountSettingsResponse currentSettings =
            new AccountSettingsResponse(
                    true,
                    true,
                    true,
                    true,
                    false,
                    false,
                    7,
                    true,
                    "en",
                    "Asia/Kolkata"
            );

    @Override
    public AccountSettingsResponse getCurrentUserSettings() {
        return currentSettings;
    }

    @Override
    public AccountSettingsResponse updateCurrentUserSettings(
            UpdateAccountSettingsRequest request
    ) {

        currentSettings =
                new AccountSettingsResponse(
                        request.emailNotifications(),
                        request.securityAlerts(),
                        request.documentReminders(),
                        request.emergencyNotifications(),
                        request.profileVisible(),
                        request.activityVisible(),
                        request.emergencyDelayDays(),
                        request.nomineeVerificationRequired(),
                        request.language(),
                        request.timezone()
                );

        return currentSettings;
    }
}