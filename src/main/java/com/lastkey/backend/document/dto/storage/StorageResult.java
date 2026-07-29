package com.lastkey.backend.document.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageResult {

    private String storedFileName;

    private boolean encrypted;

    private String encryptionAlgorithm;

    private Integer encryptionKeyVersion;

    private String initializationVector;

    private String originalChecksum;

    private String encryptedChecksum;

    private Long originalSize;

    private Long encryptedSize;

    private String mimeType;

    private String originalFileName;
}