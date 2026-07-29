package com.lastkey.backend.category.controller;

import com.lastkey.backend.category.dto.request.CreateCategoryRequest;
import com.lastkey.backend.category.dto.request.UpdateCategoryRequest;
import com.lastkey.backend.category.dto.response.CategoryResponse;
import com.lastkey.backend.category.service.CategoryService;
import com.lastkey.backend.common.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            Authentication authentication
    ) {

        CategoryResponse response =
                categoryService.createCategory(
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            Authentication authentication
    ) {

        List<CategoryResponse> response =
                categoryService.getAllCategories(
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @PathVariable UUID categoryId,
            Authentication authentication
    ) {

        CategoryResponse response =
                categoryService.getCategoryById(
                        categoryId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request,
            Authentication authentication
    ) {

        CategoryResponse response =
                categoryService.updateCategory(
                        categoryId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<MessageResponse> deleteCategory(
            @PathVariable UUID categoryId,
            Authentication authentication
    ) {

        MessageResponse response =
                categoryService.deleteCategory(
                        categoryId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{categoryId}/toggle-status")
    public ResponseEntity<MessageResponse> toggleCategoryStatus(
            @PathVariable UUID categoryId,
            Authentication authentication
    ) {

        MessageResponse response =
                categoryService.toggleCategoryStatus(
                        categoryId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }
}