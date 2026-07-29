package com.lastkey.backend.user.service;

import com.lastkey.backend.user.dto.request.UpdateAccountSettingsRequest;
import com.lastkey.backend.user.dto.response.AccountSettingsResponse;

public interface UserSettingsService {

    AccountSettingsResponse getCurrentUserSettings();

    AccountSettingsResponse updateCurrentUserSettings(
            UpdateAccountSettingsRequest request
    );
}