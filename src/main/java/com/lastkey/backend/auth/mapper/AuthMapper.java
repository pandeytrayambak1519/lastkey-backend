package com.lastkey.backend.auth.mapper;

import com.lastkey.backend.auth.dto.request.RegisterRequest;
import com.lastkey.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public User toUser(RegisterRequest request) {

        if (request == null) {
            return null;
        }

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        return user;
    }
}