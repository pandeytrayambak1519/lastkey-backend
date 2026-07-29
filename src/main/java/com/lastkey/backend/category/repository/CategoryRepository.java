package com.lastkey.backend.category.repository;

import com.lastkey.backend.category.entity.Category;
import com.lastkey.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository
        extends JpaRepository<Category, UUID> {

    List<Category>
    findBySystemCategoryTrueAndActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Category>
    findBySystemCategoryTrueOrderByDisplayOrderAscNameAsc();

    List<Category>
    findByOwnerAndActiveTrueOrderByDisplayOrderAscNameAsc(
            User owner
    );

    List<Category>
    findByOwnerOrderByDisplayOrderAscNameAsc(
            User owner
    );

    Optional<Category> findByIdAndOwner(
            UUID id,
            User owner
    );

    Optional<Category> findByIdAndOwnerAndActiveTrue(
            UUID id,
            User owner
    );

    Optional<Category> findByIdAndSystemCategoryTrue(
            UUID id
    );

    Optional<Category>
    findByIdAndSystemCategoryTrueAndActiveTrue(
            UUID id
    );

    boolean existsByNameIgnoreCaseAndOwner(
            String name,
            User owner
    );

    boolean existsByNameIgnoreCaseAndOwnerIsNull(
            String name
    );

    boolean existsByNameIgnoreCaseAndOwnerAndIdNot(
            String name,
            User owner,
            UUID id
    );

    boolean existsByNameIgnoreCaseAndOwnerIsNullAndIdNot(
            String name,
            UUID id
    );

    @Query("""
            SELECT category
            FROM Category category
            WHERE category.active = true
              AND (
                    category.systemCategory = true
                    OR category.owner = :owner
                  )
            ORDER BY category.displayOrder ASC,
                     category.name ASC
            """)
    List<Category> findAllVisibleCategories(
            @Param("owner") User owner
    );

    @Query("""
            SELECT category
            FROM Category category
            WHERE category.id = :categoryId
              AND category.active = true
              AND (
                    category.systemCategory = true
                    OR category.owner = :owner
                  )
            """)
    Optional<Category> findAccessibleCategory(
            @Param("categoryId") UUID categoryId,
            @Param("owner") User owner
    );

    /*
     * Dashboard:
     * Counts active system categories and active custom
     * categories owned by the current user.
     */
    @Query("""
            SELECT COUNT(category)
            FROM Category category
            WHERE category.active = true
              AND (
                    category.systemCategory = true
                    OR category.owner = :owner
                  )
            """)
    long countVisibleCategories(
            @Param("owner") User owner
    );
}