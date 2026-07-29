package com.lastkey.backend.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(
            max = 100,
            message = "Category name cannot exceed 100 characters"
    )
    private String name;

    @Size(
            max = 500,
            message = "Description cannot exceed 500 characters"
    )
    private String description;

    @Size(
            max = 100,
            message = "Icon cannot exceed 100 characters"
    )
    private String icon;

    @Size(
            max = 30,
            message = "Color cannot exceed 30 characters"
    )
    private String color;
}