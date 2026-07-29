package com.lastkey.backend.document.entity;

import com.lastkey.backend.category.entity.Category;
import com.lastkey.backend.document.enums.DocumentStatus;
import com.lastkey.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(
                        name = "idx_document_owner",
                        columnList = "owner_id"
                ),
                @Index(
                        name = "idx_document_category",
                        columnList = "category_id"
                ),
                @Index(
                        name = "idx_document_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_document_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_document_expiry_date",
                        columnList = "expiry_date"
                ),
                @Index(
                        name = "idx_document_ai_type",
                        columnList = "ai_document_type"
                ),
                @Index(
                        name = "idx_document_ai_review",
                        columnList = "ai_review_required"
                ),
                @Index(
                        name = "idx_document_encrypted",
                        columnList = "encrypted"
                ),
                @Index(
                        name = "idx_document_encryption_key_version",
                        columnList = "encryption_key_version"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            nullable = false,
            length = 200
    )
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(
            name = "original_file_name",
            nullable = false,
            length = 255
    )
    private String originalFileName;

    @Column(
            name = "stored_file_name",
            nullable = false,
            unique = true,
            length = 255
    )
    private String storedFileName;

    @Column(
            name = "file_type",
            nullable = false,
            length = 100
    )
    private String fileType;

    @Column(
            name = "mime_type",
            nullable = false,
            length = 150
    )
    private String mimeType;

    @Column(
            name = "file_size",
            nullable = false
    )
    private Long fileSize;

    @Column(
            name = "storage_path",
            nullable = false,
            length = 1000
    )
    private String storagePath;

    /*
     * SHA-256 checksum of the original unencrypted file.
     *
     * This checksum can be used for duplicate-document detection
     * and verification of the original file after decryption.
     */
    @Column(
            name = "checksum",
            length = 128
    )
    private String checksum;

    /*
     * =========================================================
     * FILE ENCRYPTION METADATA
     * =========================================================
     */

    /*
     * Indicates whether the file stored on disk is encrypted.
     *
     * Newly uploaded documents are encrypted before they are
     * written to permanent storage.
     */
    @Builder.Default
    @Column(
            name = "encrypted",
            nullable = false
    )
    private Boolean encrypted = true;

    /*
     * Encryption algorithm used to protect the document.
     *
     * Example:
     * AES/GCM/NoPadding
     */
    @Column(
            name = "encryption_algorithm",
            length = 100
    )
    private String encryptionAlgorithm;

    /*
     * Version of the encryption key used for this document.
     *
     * This value supports future encryption-key rotation without
     * storing the actual secret key in the database.
     *
     * Examples:
     * v1
     * v2
     * 2026-01
     */
    @Column(
            name = "encryption_key_version",
            length = 50
    )
    private String encryptionKeyVersion;

    /*
     * Base64-encoded initialization vector used by AES-GCM.
     *
     * The IV is not a secret, but it must be preserved because
     * the same IV is required when decrypting the document.
     */
    @Column(
            name = "initialization_vector",
            length = 255
    )
    private String initializationVector;

    /*
     * SHA-256 checksum of the encrypted file stored on disk.
     *
     * This allows the application to detect corruption or
     * modification of the encrypted file before decryption.
     */
    @Column(
            name = "encrypted_checksum",
            length = 128
    )
    private String encryptedChecksum;

    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean favorite = false;

    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean archived = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(
            nullable = false,
            length = 30
    )
    private DocumentStatus status = DocumentStatus.ACTIVE;

    /*
     * Optional expiry date for documents such as passports,
     * insurance policies and driving licences.
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Builder.Default
    @Column(
            name = "expiry_reminder_30_sent",
            nullable = false
    )
    private Boolean expiryReminder30Sent = false;

    @Builder.Default
    @Column(
            name = "expiry_reminder_7_sent",
            nullable = false
    )
    private Boolean expiryReminder7Sent = false;

    @Builder.Default
    @Column(
            name = "expiry_reminder_1_sent",
            nullable = false
    )
    private Boolean expiryReminder1Sent = false;

    @Builder.Default
    @Column(
            name = "expiry_notification_sent",
            nullable = false
    )
    private Boolean expiryNotificationSent = false;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_document_owner"
            )
    )
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_id",
            foreignKey = @ForeignKey(
                    name = "fk_document_category"
            )
    )
    private Category category;

    /*
     * =========================================================
     * AI DOCUMENT ANALYSIS DATA
     * =========================================================
     */

    /*
     * AI-detected document type.
     *
     * Examples:
     * PASSPORT
     * AADHAAR_CARD
     * PAN_CARD
     * INSURANCE_POLICY
     * PROPERTY_DOCUMENT
     * BANK_STATEMENT
     */
    @Column(
            name = "ai_document_type",
            length = 100
    )
    private String aiDocumentType;

    /*
     * Category suggested by the AI analysis process.
     */
    @Column(
            name = "ai_suggested_category",
            length = 150
    )
    private String aiSuggestedCategory;

    /*
     * AI-generated document summary.
     */
    @Column(
            name = "ai_summary",
            columnDefinition = "TEXT"
    )
    private String aiSummary;

    /*
     * Final combined confidence score produced by OCR,
     * classification and summarization.
     *
     * Expected range: 0.0 to 1.0
     */
    @Column(
            name = "ai_confidence"
    )
    private Double aiConfidence;

    /*
     * Text extracted from the uploaded document through OCR.
     */
    @Column(
            name = "ai_extracted_text",
            columnDefinition = "TEXT"
    )
    private String aiExtractedText;

    /*
     * JSON representation of structured fields extracted
     * from the document.
     *
     * Examples:
     * passportNumber
     * policyNumber
     * accountNumber
     * issueDate
     * expiryDate
     */
    @Column(
            name = "ai_extracted_fields_json",
            columnDefinition = "TEXT"
    )
    private String aiExtractedFieldsJson;

    /*
     * Indicates that the AI result should be manually reviewed.
     */
    @Builder.Default
    @Column(
            name = "ai_review_required",
            nullable = false
    )
    private Boolean aiReviewRequired = false;

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

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        applyDefaultValues();
        normalizeFields();
        normalizeAiConfidence();
        validateEncryptionMetadata();
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt =
                LocalDateTime.now();

        applyDefaultValues();
        normalizeFields();
        normalizeAiConfidence();
        validateEncryptionMetadata();
    }

    public void resetExpiryReminderFlags() {

        this.expiryReminder30Sent = false;
        this.expiryReminder7Sent = false;
        this.expiryReminder1Sent = false;
        this.expiryNotificationSent = false;
    }

    public void clearAiAnalysis() {

        this.aiDocumentType = null;
        this.aiSuggestedCategory = null;
        this.aiSummary = null;
        this.aiConfidence = null;
        this.aiExtractedText = null;
        this.aiExtractedFieldsJson = null;
        this.aiReviewRequired = false;
    }

    /*
     * Applies encryption metadata after a file is encrypted.
     */
    public void applyEncryptionMetadata(
            String encryptionAlgorithm,
            String encryptionKeyVersion,
            String initializationVector,
            String encryptedChecksum
    ) {

        this.encrypted = true;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.encryptionKeyVersion = encryptionKeyVersion;
        this.initializationVector = initializationVector;
        this.encryptedChecksum = encryptedChecksum;

        normalizeEncryptionFields();
    }

    /*
     * Clears encryption metadata and marks the stored file
     * as unencrypted.
     *
     * This should normally be used only for legacy documents
     * or controlled migration operations.
     */
    public void clearEncryptionMetadata() {

        this.encrypted = false;
        this.encryptionAlgorithm = null;
        this.encryptionKeyVersion = null;
        this.initializationVector = null;
        this.encryptedChecksum = null;
    }

    /*
     * Returns true only when the document is marked as encrypted
     * and all required decryption metadata is available.
     */
    public boolean hasCompleteEncryptionMetadata() {

        return Boolean.TRUE.equals(this.encrypted)
                && this.encryptionAlgorithm != null
                && !this.encryptionAlgorithm.isBlank()
                && this.encryptionKeyVersion != null
                && !this.encryptionKeyVersion.isBlank()
                && this.initializationVector != null
                && !this.initializationVector.isBlank();
    }

    private void applyDefaultValues() {

        if (this.favorite == null) {
            this.favorite = false;
        }

        if (this.archived == null) {
            this.archived = false;
        }

        if (this.encrypted == null) {
            this.encrypted = true;
        }

        if (this.status == null) {
            this.status = DocumentStatus.ACTIVE;
        }

        if (this.expiryReminder30Sent == null) {
            this.expiryReminder30Sent = false;
        }

        if (this.expiryReminder7Sent == null) {
            this.expiryReminder7Sent = false;
        }

        if (this.expiryReminder1Sent == null) {
            this.expiryReminder1Sent = false;
        }

        if (this.expiryNotificationSent == null) {
            this.expiryNotificationSent = false;
        }

        if (this.aiReviewRequired == null) {
            this.aiReviewRequired = false;
        }
    }

    private void normalizeFields() {

        if (this.title != null) {
            this.title = this.title.trim();
        }

        if (this.description != null) {
            this.description =
                    normalizeNullable(
                            this.description
                    );
        }

        if (this.originalFileName != null) {
            this.originalFileName =
                    this.originalFileName.trim();
        }

        if (this.storedFileName != null) {
            this.storedFileName =
                    this.storedFileName.trim();
        }

        if (this.fileType != null) {
            this.fileType =
                    this.fileType.trim();
        }

        if (this.mimeType != null) {
            this.mimeType =
                    this.mimeType.trim();
        }

        if (this.storagePath != null) {
            this.storagePath =
                    this.storagePath.trim();
        }

        if (this.checksum != null) {
            this.checksum =
                    normalizeNullable(
                            this.checksum
                    );
        }

        normalizeEncryptionFields();

        if (this.aiDocumentType != null) {
            this.aiDocumentType =
                    normalizeNullable(
                            this.aiDocumentType
                    );
        }

        if (this.aiSuggestedCategory != null) {
            this.aiSuggestedCategory =
                    normalizeNullable(
                            this.aiSuggestedCategory
                    );
        }

        if (this.aiSummary != null) {
            this.aiSummary =
                    normalizeNullable(
                            this.aiSummary
                    );
        }

        if (this.aiExtractedText != null) {
            this.aiExtractedText =
                    normalizeNullable(
                            this.aiExtractedText
                    );
        }

        if (this.aiExtractedFieldsJson != null) {
            this.aiExtractedFieldsJson =
                    normalizeNullable(
                            this.aiExtractedFieldsJson
                    );
        }
    }

    private void normalizeEncryptionFields() {

        if (this.encryptionAlgorithm != null) {
            this.encryptionAlgorithm =
                    normalizeNullable(
                            this.encryptionAlgorithm
                    );
        }

        if (this.encryptionKeyVersion != null) {
            this.encryptionKeyVersion =
                    normalizeNullable(
                            this.encryptionKeyVersion
                    );
        }

        if (this.initializationVector != null) {
            this.initializationVector =
                    normalizeNullable(
                            this.initializationVector
                    );
        }

        if (this.encryptedChecksum != null) {
            this.encryptedChecksum =
                    normalizeNullable(
                            this.encryptedChecksum
                    );
        }
    }

    private void normalizeAiConfidence() {

        if (this.aiConfidence == null) {
            return;
        }

        if (this.aiConfidence < 0.0) {
            this.aiConfidence = 0.0;
        }

        if (this.aiConfidence > 1.0) {
            this.aiConfidence = 1.0;
        }
    }

    /*
     * Prevents unencrypted documents from retaining stale
     * encryption metadata.
     *
     * Complete metadata validation will be handled by the
     * encryption service because legacy documents may initially
     * be migrated without all fields being available.
     */
    private void validateEncryptionMetadata() {

        if (Boolean.FALSE.equals(this.encrypted)) {
            this.encryptionAlgorithm = null;
            this.encryptionKeyVersion = null;
            this.initializationVector = null;
            this.encryptedChecksum = null;
        }
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalizedValue =
                value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}