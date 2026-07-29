package com.lastkey.backend.security.encryption.service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DecryptionResult {

    private final boolean decrypted;

    private final String algorithm;

    private final String keyVersion;

    private final String decryptedChecksum;

    private final long encryptedSize;

    private final long decryptedSize;
}