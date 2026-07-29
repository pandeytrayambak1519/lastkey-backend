package com.lastkey.backend.nominee.entity;

import com.lastkey.backend.document.entity.Document;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "nominee_document_access",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_nominee_document",
                        columnNames = {
                                "nominee_id",
                                "document_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_nominee_access_nominee",
                        columnList = "nominee_id"
                ),
                @Index(
                        name = "idx_nominee_access_document",
                        columnList = "document_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomineeDocumentAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "nominee_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_nominee_document_nominee"
            )
    )
    private Nominee nominee;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_nominee_document_document"
            )
    )
    private Document document;

    @Column(
            name = "can_view",
            nullable = false
    )
    @Builder.Default
    private Boolean canView = true;

    @Column(
            name = "can_download",
            nullable = false
    )
    @Builder.Default
    private Boolean canDownload = true;

    @Column(
            name = "granted_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime grantedAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        grantedAt = now;
        updatedAt = now;

        if (canView == null) {
            canView = true;
        }

        if (canDownload == null) {
            canDownload = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}