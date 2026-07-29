package com.lastkey.backend.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid recipient email")
    private String recipient;

    @NotBlank(message = "Email subject is required")
    private String subject;

    @NotBlank(message = "Email message is required")
    private String message;
}