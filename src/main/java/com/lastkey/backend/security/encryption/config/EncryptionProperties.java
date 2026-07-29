package com.lastkey.backend.security.encryption.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    /*
     * Enables or disables document encryption.
     */
    private boolean enabled = true;

    /*
     * Authenticated encryption algorithm.
     */
    @NotBlank
    private String algorithm = "AES/GCM/NoPadding";

    /*
     * Base64-encoded AES encryption key.
     *
     * AES-256 requires exactly 32 decoded bytes.
     */
    @NotBlank
    private String key;

    /*
     * Identifies the key used to encrypt documents.
     * This supports future encryption-key rotation.
     */
    @NotBlank
    private String keyVersion = "v1";

    /*
     * AES-GCM standard IV length in bytes.
     */
    @Min(12)
    private int ivLength = 12;

    /*
     * AES-GCM authentication tag length in bits.
     */
    @Min(96)
    private int authenticationTagLength = 128;

    /*
     * Buffer used for stream-based encryption and decryption.
     */
    @Min(1024)
    private int bufferSize = 8192;

    @PostConstruct
    public void validateConfiguration() {

        if (!enabled) {
            return;
        }

        validateAlgorithm();
        validateKeyVersion();
        validateInitializationVectorLength();
        validateAuthenticationTagLength();
        validateBufferSize();
        validateEncryptionKey();
    }

    public byte[] getDecodedKey() {

        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Encryption key is not configured."
            );
        }

        try {
            return Base64.getDecoder().decode(key.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Encryption key must be a valid Base64-encoded value.",
                    exception
            );
        }
    }

    private void validateAlgorithm() {

        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalStateException(
                    "Encryption algorithm must not be empty."
            );
        }

        algorithm = algorithm.trim();

        if (!"AES/GCM/NoPadding".equalsIgnoreCase(algorithm)) {
            throw new IllegalStateException(
                    "Unsupported encryption algorithm: "
                            + algorithm
                            + ". Required algorithm: AES/GCM/NoPadding."
            );
        }

        algorithm = "AES/GCM/NoPadding";
    }

    private void validateKeyVersion() {

        if (keyVersion == null || keyVersion.isBlank()) {
            throw new IllegalStateException(
                    "Encryption key version must not be empty."
            );
        }

        keyVersion = keyVersion.trim();

        if (keyVersion.length() > 50) {
            throw new IllegalStateException(
                    "Encryption key version must not exceed 50 characters."
            );
        }
    }

    private void validateInitializationVectorLength() {

        if (ivLength != 12) {
            throw new IllegalStateException(
                    "AES-GCM initialization vector length must be 12 bytes."
            );
        }
    }

    private void validateAuthenticationTagLength() {

        if (authenticationTagLength != 128) {
            throw new IllegalStateException(
                    "AES-GCM authentication tag length must be 128 bits."
            );
        }
    }

    private void validateBufferSize() {

        if (bufferSize < 1024) {
            throw new IllegalStateException(
                    "Encryption buffer size must be at least 1024 bytes."
            );
        }
    }

    private void validateEncryptionKey() {

        byte[] decodedKey = getDecodedKey();

        if (decodedKey.length != 32) {
            throw new IllegalStateException(
                    "AES-256 encryption requires a Base64 key that decodes "
                            + "to exactly 32 bytes. Current decoded length: "
                            + decodedKey.length
                            + " bytes."
            );
        }
    }
}