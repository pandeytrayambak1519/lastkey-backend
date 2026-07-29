package com.lastkey.backend.category.service.impl;

import com.lastkey.backend.category.dto.request.CreateCategoryRequest;
import com.lastkey.backend.category.dto.request.UpdateCategoryRequest;
import com.lastkey.backend.category.dto.response.CategoryResponse;
import com.lastkey.backend.category.entity.Category;
import com.lastkey.backend.category.exception.CategoryAlreadyExistsException;
import com.lastkey.backend.category.exception.CategoryNotFoundException;
import com.lastkey.backend.category.exception.SystemCategoryModificationException;
import com.lastkey.backend.category.mapper.CategoryMapper;
import com.lastkey.backend.category.repository.CategoryRepository;
import com.lastkey.backend.category.service.CategoryService;
import com.lastkey.backend.common.dto.MessageResponse;
import com.lastkey.backend.notification.service.NotificationEventService;
import com.lastkey.backend.user.entity.User;
import com.lastkey.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;
    private final NotificationEventService notificationEventService;

    @Override
    public CategoryResponse createCategory(
            CreateCategoryRequest request,
            String email
    ) {

        User currentUser = getUserByEmail(email);

        String normalizedName = request.getName().trim();

        boolean categoryExists =
                categoryRepository.existsByNameIgnoreCaseAndOwner(
                        normalizedName,
                        currentUser
                );

        if (categoryExists) {
            throw new CategoryAlreadyExistsException(
                    "You already have a category with this name."
            );
        }

        Category category = categoryMapper.toEntity(request);

        category.setOwner(currentUser);
        category.setSystemCategory(false);
        category.setActive(true);

        Category savedCategory =
                categoryRepository.save(category);

        notificationEventService.categoryCreated(savedCategory);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(
            String email
    ) {

        User currentUser = getUserByEmail(email);

        return categoryRepository
                .findAllVisibleCategories(currentUser)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(
            UUID categoryId,
            String email
    ) {

        User currentUser = getUserByEmail(email);

        Category category = categoryRepository
                .findAccessibleCategory(
                        categoryId,
                        currentUser
                )
                .orElseThrow(
                        () -> new CategoryNotFoundException(
                                "Category not found or you do not have access."
                        )
                );

        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(
            UUID categoryId,
            UpdateCategoryRequest request,
            String email
    ) {

        User currentUser = getUserByEmail(email);

        Category category = getOwnedCategory(
                categoryId,
                currentUser
        );

        validateNotSystemCategory(category);

        String normalizedName = request.getName().trim();

        boolean duplicateExists =
                categoryRepository
                        .existsByNameIgnoreCaseAndOwnerAndIdNot(
                                normalizedName,
                                currentUser,
                                categoryId
                        );

        if (duplicateExists) {
            throw new CategoryAlreadyExistsException(
                    "You already have another category with this name."
            );
        }

        categoryMapper.updateEntity(
                category,
                request
        );

        Category updatedCategory =
                categoryRepository.save(category);

        notificationEventService.categoryUpdated(updatedCategory);

        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public MessageResponse deleteCategory(
            UUID categoryId,
            String email
    ) {

        User currentUser = getUserByEmail(email);

        Category category = getOwnedCategory(
                categoryId,
                currentUser
        );

        validateNotSystemCategory(category);

        if (Boolean.FALSE.equals(category.getActive())) {
            return MessageResponse.builder()
                    .message("Category is already inactive.")
                    .build();
        }

        category.setActive(false);

        Category deletedCategory =
                categoryRepository.save(category);

        notificationEventService.categoryDeleted(deletedCategory);

        return MessageResponse.builder()
                .message("Category deleted successfully.")
                .build();
    }

    @Override
    public MessageResponse toggleCategoryStatus(
            UUID categoryId,
            String email
    ) {

        User currentUser = getUserByEmail(email);

        Category category = getOwnedCategory(
                categoryId,
                currentUser
        );

        validateNotSystemCategory(category);

        boolean newStatus =
                !Boolean.TRUE.equals(category.getActive());

        category.setActive(newStatus);

        categoryRepository.save(category);

        String message = newStatus
                ? "Category activated successfully."
                : "Category deactivated successfully.";

        return MessageResponse.builder()
                .message(message)
                .build();
    }

    private User getUserByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required."
            );
        }

        return userRepository
                .findByEmail(email.trim())
                .orElseThrow(
                        () -> new RuntimeException(
                                "Authenticated user not found."
                        )
                );
    }

    private Category getOwnedCategory(
            UUID categoryId,
            User currentUser
    ) {

        return categoryRepository
                .findByIdAndOwner(
                        categoryId,
                        currentUser
                )
                .orElseThrow(
                        () -> new CategoryNotFoundException(
                                "Category not found or it does not belong to you."
                        )
                );
    }

    private void validateNotSystemCategory(
            Category category
    ) {

        if (Boolean.TRUE.equals(
                category.getSystemCategory()
        )) {
            throw new SystemCategoryModificationException(
                    "System categories cannot be modified."
            );
        }
    }
}