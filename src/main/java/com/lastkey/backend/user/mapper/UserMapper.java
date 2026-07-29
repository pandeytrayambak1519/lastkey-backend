package com.lastkey.backend.user.mapper;

import com.lastkey.backend.user.dto.response.UserProfileResponse;
import com.lastkey.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {

        if (user == null) {
            return null;
        }

        UserProfileResponse response = new UserProfileResponse();

        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setProfileImageUrl(user.getProfileImageUrl());
        response.setEmailVerified(user.getEmailVerified());
        response.setAccountStatus(user.getAccountStatus());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        if (user.getRole() != null && user.getRole().getName() != null) {
            response.setRole(user.getRole().getName().name());
        }

        return response;
    }
}