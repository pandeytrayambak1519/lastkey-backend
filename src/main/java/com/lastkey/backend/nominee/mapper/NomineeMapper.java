package com.lastkey.backend.nominee.mapper;

import com.lastkey.backend.nominee.dto.request.NomineeCreateRequest;
import com.lastkey.backend.nominee.dto.response.NomineeResponse;
import com.lastkey.backend.nominee.entity.Nominee;
import com.lastkey.backend.nominee.enums.RelationshipType;
import org.springframework.stereotype.Component;

@Component
public class NomineeMapper {

    public Nominee toEntity(NomineeCreateRequest request) {

        return Nominee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .relationship(request.getRelationship())
                .customRelationship(
                        request.getRelationship() == RelationshipType.OTHER
                                ? request.getCustomRelationship()
                                : null
                )
                .primaryNominee(
                        Boolean.TRUE.equals(
                                request.getPrimaryNominee()
                        )
                )
                .notes(request.getNotes())
                .build();
    }

    public NomineeResponse toResponse(
            Nominee nominee,
            long assignedDocumentCount
    ) {

        return NomineeResponse.builder()
                .id(nominee.getId())
                .firstName(nominee.getFirstName())
                .lastName(nominee.getLastName())
                .fullName(buildFullName(nominee))
                .email(nominee.getEmail())
                .phone(nominee.getPhone())
                .relationship(nominee.getRelationship())
                .relationshipDisplayName(
                        getRelationshipDisplayName(nominee)
                )
                .customRelationship(
                        nominee.getCustomRelationship()
                )
                .status(nominee.getStatus())
                .primaryNominee(
                        nominee.getPrimaryNominee()
                )
                .emailVerified(
                        nominee.getEmailVerified()
                )
                .phoneVerified(
                        nominee.getPhoneVerified()
                )
                .active(nominee.getActive())
                .notes(nominee.getNotes())
                .assignedDocumentCount(
                        assignedDocumentCount
                )
                .verifiedAt(nominee.getVerifiedAt())
                .createdAt(nominee.getCreatedAt())
                .updatedAt(nominee.getUpdatedAt())
                .build();
    }

    private String buildFullName(Nominee nominee) {

        String firstName =
                nominee.getFirstName() == null
                        ? ""
                        : nominee.getFirstName().trim();

        String lastName =
                nominee.getLastName() == null
                        ? ""
                        : nominee.getLastName().trim();

        return (firstName + " " + lastName).trim();
    }

    private String getRelationshipDisplayName(
            Nominee nominee
    ) {

        if (nominee.getRelationship() == null) {
            return null;
        }

        if (nominee.getRelationship()
                == RelationshipType.OTHER) {

            return nominee.getCustomRelationship() == null
                    ? "Other"
                    : nominee.getCustomRelationship();
        }

        String value =
                nominee.getRelationship().name()
                        .toLowerCase()
                        .replace("_", " ");

        StringBuilder formatted =
                new StringBuilder();

        for (String word : value.split(" ")) {

            if (!formatted.isEmpty()) {
                formatted.append(" ");
            }

            formatted.append(
                    Character.toUpperCase(
                            word.charAt(0)
                    )
            );

            formatted.append(
                    word.substring(1)
            );
        }

        return formatted.toString();
    }
}