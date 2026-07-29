package com.lastkey.backend.category.service;

import com.lastkey.backend.category.dto.request.CreateCategoryRequest;
import com.lastkey.backend.category.dto.request.UpdateCategoryRequest;
import com.lastkey.backend.category.dto.response.CategoryResponse;
import com.lastkey.backend.common.dto.MessageResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(
            CreateCategoryRequest request,
            String email
    );

    List<CategoryResponse> getAllCategories(
            String email
    );

    CategoryResponse getCategoryById(
            UUID categoryId,
            String email
    );
    CategoryResponse updateCategory(
            UUID categoryId,
            UpdateCategoryRequest request,
            String email
    );
    MessageResponse deleteCategory(
            UUID categoryId,
            String email
    );

    MessageResponse toggleCategoryStatus(
            UUID categoryId,
            String email
    );
}