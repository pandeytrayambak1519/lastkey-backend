package com.lastkey.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeactivateAccountRequest(

        @NotBlank(message = "Password is required")
        String password

) {
}