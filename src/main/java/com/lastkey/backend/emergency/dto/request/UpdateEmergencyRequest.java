package com.lastkey.backend.emergency.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmergencyRequest {

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

    @Min(
            value = 1,
            message = "Waiting period must be at least 1 day"
    )
    @Max(
            value = 30,
            message = "Waiting period cannot exceed 30 days"
    )
    private Integer waitingPeriodDays;
}