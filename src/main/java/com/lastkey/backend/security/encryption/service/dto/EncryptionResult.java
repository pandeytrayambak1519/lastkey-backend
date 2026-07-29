package com.lastkey.backend.security.encryption.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EncryptionResult {

    private final boolean encrypted;

    private final String algorithm;

    private final String keyVersion;

    private final String initializationVector;

    private final String originalChecksum;

    private final String encryptedChecksum;

    private final long originalSize;

    private final long encryptedSize;
}