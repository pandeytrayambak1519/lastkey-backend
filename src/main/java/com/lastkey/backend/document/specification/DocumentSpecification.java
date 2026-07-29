package com.lastkey.backend.document.specification;

import com.lastkey.backend.document.dto.request.DocumentSearchRequest;
import com.lastkey.backend.document.entity.Document;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class DocumentSpecification {

    private DocumentSpecification() {
    }

    public static Specification<Document> build(
            User owner,
            DocumentSearchRequest request
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            /*
             * Security condition:
             * A user can search only their own documents.
             */
            predicates.add(
                    criteriaBuilder.equal(
                            root.get("owner"),
                            owner
                    )
            );

            /*
             * Return only active documents.
             */
            predicates.add(
                    criteriaBuilder.equal(
                            root.get("status"),
                            DocumentStatus.ACTIVE
                    )
            );

            if (hasText(request.getKeyword())) {

                String searchKeyword =
                        "%"
                                + request.getKeyword()
                                .trim()
                                .toLowerCase()
                                + "%";

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("title")
                                ),
                                searchKeyword
                        )
                );
            }

            if (request.getCategoryId() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("category")
                                        .get("id"),
                                request.getCategoryId()
                        )
                );
            }

            if (hasText(request.getFileType())) {

                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(
                                        root.get("fileType")
                                ),
                                request.getFileType()
                                        .trim()
                                        .toLowerCase()
                        )
                );
            }

            if (request.getFavorite() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("favorite"),
                                request.getFavorite()
                        )
                );
            }

            if (request.getArchived() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("archived"),
                                request.getArchived()
                        )
                );
            }

            if (request.getExpiryFrom() != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("expiryDate"),
                                request.getExpiryFrom()
                        )
                );
            }

            if (request.getExpiryTo() != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("expiryDate"),
                                request.getExpiryTo()
                        )
                );
            }

            if (request.getCreatedFrom() != null) {

                LocalDateTime createdFrom =
                        request.getCreatedFrom()
                                .atStartOfDay();

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                createdFrom
                        )
                );
            }

            if (request.getCreatedTo() != null) {

                LocalDateTime createdTo =
                        request.getCreatedTo()
                                .atTime(LocalTime.MAX);

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdAt"),
                                createdTo
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.trim().isEmpty();
    }
}