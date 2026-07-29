package com.lastkey.backend.category.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private UUID id;

    private String name;

    private String description;

    private String icon;

    private String color;

    private Integer displayOrder;

    private Boolean systemCategory;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}