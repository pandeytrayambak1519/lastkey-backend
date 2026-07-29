package com.lastkey.backend.emergency.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyActionRequest {

    @Size(
            max = 1000,
            message = "Message cannot exceed 1000 characters"
    )
    private String message;
}