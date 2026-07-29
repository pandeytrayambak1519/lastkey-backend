package com.lastkey.backend.category.mapper;

import com.lastkey.backend.category.dto.request.CreateCategoryRequest;
import com.lastkey.backend.category.dto.request.UpdateCategoryRequest;
import com.lastkey.backend.category.dto.response.CategoryResponse;
import com.lastkey.backend.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest request) {

        if (request == null) {
            return null;
        }

        return Category.builder()
                .name(normalize(request.getName()))
                .description(normalizeNullable(request.getDescription()))
                .icon(normalizeNullable(request.getIcon()))
                .color(normalizeNullable(request.getColor()))
                .displayOrder(
                        request.getDisplayOrder() != null
                                ? request.getDisplayOrder()
                                : 0
                )
                .systemCategory(false)
                .active(true)
                .build();
    }

    public CategoryResponse toResponse(Category category) {

        if (category == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .displayOrder(category.getDisplayOrder())
                .systemCategory(category.getSystemCategory())
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public void updateEntity(
            Category category,
            UpdateCategoryRequest request
    ) {

        if (category == null || request == null) {
            return;
        }

        category.setName(normalize(request.getName()));
        category.setDescription(
                normalizeNullable(request.getDescription())
        );
        category.setIcon(
                normalizeNullable(request.getIcon())
        );
        category.setColor(
                normalizeNullable(request.getColor())
        );

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(
                    request.getDisplayOrder()
            );
        }

        if (request.getActive() != null) {
            category.setActive(
                    request.getActive()
            );
        }
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}