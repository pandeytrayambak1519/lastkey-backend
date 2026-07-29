package com.lastkey.backend.auth.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private UUID userId;

    private String firstName;

    private String lastName;

    private String email;

    private String role;

    private Boolean emailVerified;

}