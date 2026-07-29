package com.lastkey.backend.document.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastkey.backend.document.dto.response.DocumentResponse;
import com.lastkey.backend.document.dto.response.DocumentSearchItemResponse;
import com.lastkey.backend.document.entity.Document;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class DocumentMapper {

    private final ObjectMapper objectMapper;

    public DocumentMapper(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    /*
     * =========================================================
     * COMPLETE DOCUMENT RESPONSE
     * =========================================================
     */

    public DocumentResponse toResponse(
            Document document
    ) {

        if (document == null) {
            return null;
        }

        return DocumentResponse.builder()

                /*
                 * =========================================================
                 * BASIC INFORMATION
                 * =========================================================
                 */

                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .originalFileName(document.getOriginalFileName())
                .fileType(document.getFileType())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .checksum(document.getChecksum())

                /*
                 * =========================================================
                 * FLAGS
                 * =========================================================
                 */

                .encrypted(document.getEncrypted())
                .favorite(document.getFavorite())
                .archived(document.getArchived())

                .status(
                        document.getStatus() != null
                                ? document.getStatus().name()
                                : null
                )

                /*
                 * =========================================================
                 * CATEGORY
                 * =========================================================
                 */

                .categoryId(
                        document.getCategory() != null
                                ? document.getCategory().getId()
                                : null
                )

                .categoryName(
                        document.getCategory() != null
                                ? document.getCategory().getName()
                                : null
                )

                .aiSuggestedCategory(
                        document.getAiSuggestedCategory()
                )

                /*
                 * =========================================================
                 * OWNER
                 * =========================================================
                 */

                .ownerId(
                        document.getOwner() != null
                                ? document.getOwner().getId()
                                : null
                )

                /*
                 * =========================================================
                 * EXPIRY
                 * =========================================================
                 */

                .expiryDate(
                        document.getExpiryDate()
                )

                /*
                 * =========================================================
                 * AI ANALYSIS
                 * =========================================================
                 */

                .aiDocumentType(
                        document.getAiDocumentType()
                )

                .aiSummary(
                        document.getAiSummary()
                )

                .aiExtractedText(
                        document.getAiExtractedText()
                )

                .aiConfidence(
                        document.getAiConfidence()
                )

                .aiReviewRequired(
                        document.getAiReviewRequired()
                )

                .extractedFields(
                        parseExtractedFields(
                                document.getAiExtractedFieldsJson()
                        )
                )

                /*
                 * =========================================================
                 * AUDIT
                 * =========================================================
                 */

                .createdAt(
                        document.getCreatedAt()
                )

                .updatedAt(
                        document.getUpdatedAt()
                )

                .build();
    }

    /*
     * =========================================================
     * ADVANCED SEARCH ITEM RESPONSE
     * =========================================================
     */

    public DocumentSearchItemResponse toSearchItemResponse(
            Document document
    ) {

        if (document == null) {
            return null;
        }

        return DocumentSearchItemResponse.builder()

                /*
                 * =========================================================
                 * BASIC INFORMATION
                 * =========================================================
                 */

                .id(
                        document.getId()
                )

                .title(
                        document.getTitle()
                )

                .description(
                        document.getDescription()
                )

                /*
                 * =========================================================
                 * CATEGORY
                 * =========================================================
                 */

                .categoryId(
                        document.getCategory() != null
                                ? document.getCategory().getId()
                                : null
                )

                .categoryName(
                        document.getCategory() != null
                                ? document.getCategory().getName()
                                : null
                )

                /*
                 * =========================================================
                 * FILE INFORMATION
                 * =========================================================
                 */

                .originalFileName(
                        document.getOriginalFileName()
                )

                .fileType(
                        document.getFileType()
                )

                .fileSize(
                        document.getFileSize()
                )

                /*
                 * =========================================================
                 * DOCUMENT FLAGS
                 * =========================================================
                 */

                .favorite(
                        document.getFavorite()
                )

                .archived(
                        document.getArchived()
                )

                .status(
                        document.getStatus()
                )

                /*
                 * =========================================================
                 * EXPIRY
                 * =========================================================
                 */

                .expiryDate(
                        document.getExpiryDate()
                )

                /*
                 * =========================================================
                 * AUDIT
                 * =========================================================
                 */

                .createdAt(
                        document.getCreatedAt()
                )

                .updatedAt(
                        document.getUpdatedAt()
                )

                .build();
    }

    /*
     * =========================================================
     * AI EXTRACTED FIELDS JSON CONVERTER
     * =========================================================
     */

    private Map<String, Object> parseExtractedFields(
            String json
    ) {

        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }

        try {

            return objectMapper.readValue(
                    json,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

        } catch (Exception exception) {

            return Collections.emptyMap();
        }
    }
}