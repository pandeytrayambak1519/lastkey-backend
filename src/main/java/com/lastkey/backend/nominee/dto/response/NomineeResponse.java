package com.lastkey.backend.nominee.dto.response;

import com.lastkey.backend.nominee.enums.NomineeStatus;
import com.lastkey.backend.nominee.enums.RelationshipType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class NomineeResponse {

    private UUID id;

    private String firstName;

    private String lastName;

    private String fullName;

    private String email;

    private String phone;

    private RelationshipType relationship;

    private String relationshipDisplayName;

    private String customRelationship;

    private NomineeStatus status;

    private Boolean primaryNominee;

    private Boolean emailVerified;

    private Boolean phoneVerified;

    private Boolean active;

    private String notes;

    private Long assignedDocumentCount;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}