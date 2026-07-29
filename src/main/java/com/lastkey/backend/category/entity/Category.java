package com.lastkey.backend.category.entity;

import com.lastkey.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_name_owner",
                        columnNames = {"name", "owner_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String icon;

    @Column(length = 30)
    private String color;

    @Builder.Default
    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(
            name = "system_category",
            nullable = false
    )
    private Boolean systemCategory = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }

        if (this.systemCategory == null) {
            this.systemCategory = false;
        }

        if (this.active == null) {
            this.active = true;
        }

        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeFields();
    }

    private void normalizeFields() {

        if (this.name != null) {
            this.name = this.name.trim();
        }

        if (this.description != null) {
            this.description = this.description.trim();
        }

        if (this.icon != null) {
            this.icon = this.icon.trim();
        }

        if (this.color != null) {
            this.color = this.color.trim();
        }
    }
}