package com.lastkey.backend.emergency.dto.request;

import com.lastkey.backend.emergency.enums.EmergencyTriggerType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmergencyRequest {

    @NotNull(message = "Nominee ID is required")
    private UUID nomineeId;

    @NotBlank(message = "Reason is required")
    @Size(
            min = 10,
            max = 1000,
            message = "Reason must be between 10 and 1000 characters"
    )
    private String reason;

    @Size(
            max = 500,
            message = "Evidence URL cannot exceed 500 characters"
    )
    private String evidenceUrl;

    @NotNull(message = "Trigger type is required")
    private EmergencyTriggerType triggerType;

    @Min(
            value = 1,
            message = "Waiting period must be at least 1 day"
    )
    @Max(
            value = 30,
            message = "Waiting period cannot exceed 30 days"
    )
    @Builder.Default
    private Integer waitingPeriodDays = 7;
}