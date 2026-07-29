package com.lastkey.backend.user.controller;

import com.lastkey.backend.user.dto.request.UpdateAccountSettingsRequest;
import com.lastkey.backend.user.dto.response.AccountSettingsResponse;
import com.lastkey.backend.user.service.UserSettingsService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(
            UserSettingsService userSettingsService
    ) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public ResponseEntity<AccountSettingsResponse> getAccountSettings() {

        AccountSettingsResponse response =
                userSettingsService.getCurrentUserSettings();

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<AccountSettingsResponse> updateAccountSettings(
            @Valid @RequestBody UpdateAccountSettingsRequest request
    ) {

        AccountSettingsResponse response =
                userSettingsService.updateCurrentUserSettings(request);

        return ResponseEntity.ok(response);
    }
}