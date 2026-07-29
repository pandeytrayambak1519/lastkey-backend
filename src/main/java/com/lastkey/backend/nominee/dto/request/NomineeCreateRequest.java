package com.lastkey.backend.nominee.dto.request;

import com.lastkey.backend.nominee.enums.RelationshipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NomineeCreateRequest {

    @NotBlank(message = "First name is required")
    @Size(
            min = 2,
            max = 100,
            message = "First name must be between 2 and 100 characters"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(
            min = 2,
            max = 100,
            message = "Last name must be between 2 and 100 characters"
    )
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(
            max = 150,
            message = "Email cannot exceed 150 characters"
    )
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Please provide a valid 10-digit Indian mobile number"
    )
    private String phone;

    @NotNull(message = "Relationship is required")
    private RelationshipType relationship;

    @Size(
            max = 100,
            message = "Custom relationship cannot exceed 100 characters"
    )
    private String customRelationship;

    private Boolean primaryNominee = false;

    @Size(
            max = 500,
            message = "Notes cannot exceed 500 characters"
    )
    private String notes;
}