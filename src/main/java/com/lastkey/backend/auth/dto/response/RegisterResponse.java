package com.lastkey.backend.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {

    private String message;

    private String email;

    private Boolean emailVerified;

}