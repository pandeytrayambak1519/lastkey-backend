package com.lastkey.backend.security.encryption.util;

import com.lastkey.backend.security.encryption.exception.EncryptionException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class EncryptionUtils {

    private static final String AES_ALGORITHM = "AES";
    private static final String SHA_256_ALGORITHM = "SHA-256";

    private static final int AES_256_KEY_LENGTH_BYTES = 32;
    private static final int DEFAULT_IV_LENGTH_BYTES = 12;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private EncryptionUtils() {
        throw new IllegalStateException(
                "EncryptionUtils is a utility class and cannot be instantiated."
        );
    }

    /**
     * Creates an AES-256 SecretKey from a Base64-encoded key.
     *
     * @param base64Key Base64-encoded AES key
     * @return AES SecretKey
     */
    public static SecretKey createSecretKey(
            String base64Key
    ) {

        byte[] decodedKey =
                decodeBase64Key(base64Key);

        validateAes256KeyLength(decodedKey);

        return new SecretKeySpec(
                decodedKey,
                AES_ALGORITHM
        );
    }

    /**
     * Decodes a Base64-encoded encryption key.
     *
     * @param base64Key Base64 key
     * @return decoded key bytes
     */
    public static byte[] decodeBase64Key(
            String base64Key
    ) {

        if (base64Key == null || base64Key.isBlank()) {
            throw new EncryptionException(
                    "Encryption key must not be null or empty."
            );
        }

        try {
            return Base64
                    .getDecoder()
                    .decode(base64Key.trim());

        } catch (IllegalArgumentException exception) {
            throw new EncryptionException(
                    "Encryption key must be a valid Base64-encoded value.",
                    exception
            );
        }
    }

    /**
     * Validates that a decoded key contains exactly 32 bytes,
     * which is required for AES-256.
     *
     * @param keyBytes decoded encryption-key bytes
     */
    public static void validateAes256KeyLength(
            byte[] keyBytes
    ) {

        if (keyBytes == null) {
            throw new EncryptionException(
                    "Encryption key bytes must not be null."
            );
        }

        if (keyBytes.length != AES_256_KEY_LENGTH_BYTES) {
            throw new EncryptionException(
                    "AES-256 requires an encryption key of exactly "
                            + AES_256_KEY_LENGTH_BYTES
                            + " bytes. Current key length: "
                            + keyBytes.length
                            + " bytes."
            );
        }
    }

    /**
     * Generates a cryptographically secure 12-byte IV suitable
     * for AES-GCM.
     *
     * @return random initialization vector
     */
    public static byte[] generateInitializationVector() {
        return generateInitializationVector(
                DEFAULT_IV_LENGTH_BYTES
        );
    }

    /**
     * Generates a cryptographically secure IV of the requested size.
     *
     * AES-GCM normally uses a 12-byte IV.
     *
     * @param ivLength initialization-vector length in bytes
     * @return random initialization vector
     */
    public static byte[] generateInitializationVector(
            int ivLength
    ) {

        if (ivLength <= 0) {
            throw new EncryptionException(
                    "Initialization-vector length must be greater than zero."
            );
        }

        byte[] initializationVector =
                new byte[ivLength];

        SECURE_RANDOM.nextBytes(
                initializationVector
        );

        return initializationVector;
    }

    /**
     * Encodes bytes using standard Base64.
     *
     * @param value byte array
     * @return Base64 string
     */
    public static String encodeBase64(
            byte[] value
    ) {

        if (value == null) {
            throw new EncryptionException(
                    "Cannot Base64-encode a null value."
            );
        }

        return Base64
                .getEncoder()
                .encodeToString(value);
    }

    /**
     * Decodes a standard Base64 string.
     *
     * @param encodedValue Base64 string
     * @return decoded bytes
     */
    public static byte[] decodeBase64(
            String encodedValue
    ) {

        if (encodedValue == null || encodedValue.isBlank()) {
            throw new EncryptionException(
                    "Base64 value must not be null or empty."
            );
        }

        try {
            return Base64
                    .getDecoder()
                    .decode(encodedValue.trim());

        } catch (IllegalArgumentException exception) {
            throw new EncryptionException(
                    "Value is not valid Base64.",
                    exception
            );
        }
    }

    /**
     * Calculates a SHA-256 checksum for a byte array.
     *
     * @param data input bytes
     * @return lowercase hexadecimal SHA-256 checksum
     */
    public static String calculateSha256(
            byte[] data
    ) {

        if (data == null) {
            throw new EncryptionException(
                    "Cannot calculate checksum for null data."
            );
        }

        MessageDigest digest =
                createSha256Digest();

        byte[] checksum =
                digest.digest(data);

        return toHex(checksum);
    }

    /**
     * Calculates a SHA-256 checksum for a file.
     *
     * @param filePath path of file
     * @return lowercase hexadecimal SHA-256 checksum
     */
    public static String calculateSha256(
            Path filePath
    ) {

        validateReadableFile(filePath);

        try (
                InputStream inputStream =
                        new BufferedInputStream(
                                Files.newInputStream(filePath)
                        )
        ) {

            return calculateSha256(
                    inputStream,
                    DEFAULT_BUFFER_SIZE
            );

        } catch (IOException exception) {
            throw new EncryptionException(
                    "Failed to calculate SHA-256 checksum for file: "
                            + filePath,
                    exception
            );
        }
    }

    /**
     * Calculates a SHA-256 checksum from an input stream.
     *
     * This method consumes the stream but does not close it.
     *
     * @param inputStream source stream
     * @return lowercase hexadecimal SHA-256 checksum
     */
    public static String calculateSha256(
            InputStream inputStream
    ) {

        return calculateSha256(
                inputStream,
                DEFAULT_BUFFER_SIZE
        );
    }

    /**
     * Calculates a SHA-256 checksum from an input stream using
     * the provided buffer size.
     *
     * This method consumes the stream but does not close it.
     *
     * @param inputStream source stream
     * @param bufferSize  buffer size in bytes
     * @return lowercase hexadecimal SHA-256 checksum
     */
    public static String calculateSha256(
            InputStream inputStream,
            int bufferSize
    ) {

        if (inputStream == null) {
            throw new EncryptionException(
                    "Input stream must not be null."
            );
        }

        if (bufferSize < 1024) {
            throw new EncryptionException(
                    "Checksum buffer size must be at least 1024 bytes."
            );
        }

        MessageDigest digest =
                createSha256Digest();

        byte[] buffer =
                new byte[bufferSize];

        int bytesRead;

        try {
            while (
                    (bytesRead =
                            inputStream.read(buffer)) != -1
            ) {

                if (bytesRead > 0) {
                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            return toHex(
                    digest.digest()
            );

        } catch (IOException exception) {
            throw new EncryptionException(
                    "Failed to calculate SHA-256 checksum from input stream.",
                    exception
            );
        }
    }

    /**
     * Converts bytes to a lowercase hexadecimal string.
     *
     * @param value byte array
     * @return hexadecimal string
     */
    public static String toHex(
            byte[] value
    ) {

        if (value == null) {
            throw new EncryptionException(
                    "Cannot convert null bytes to hexadecimal."
            );
        }

        return HexFormat
                .of()
                .formatHex(value);
    }

    /**
     * Converts a hexadecimal string into bytes.
     *
     * @param hexadecimal hexadecimal value
     * @return decoded bytes
     */
    public static byte[] fromHex(
            String hexadecimal
    ) {

        if (hexadecimal == null || hexadecimal.isBlank()) {
            throw new EncryptionException(
                    "Hexadecimal value must not be null or empty."
            );
        }

        try {
            return HexFormat
                    .of()
                    .parseHex(hexadecimal.trim());

        } catch (IllegalArgumentException exception) {
            throw new EncryptionException(
                    "Invalid hexadecimal value.",
                    exception
            );
        }
    }

    /**
     * Performs a constant-time comparison of two strings.
     *
     * This is useful when comparing checksums or security-sensitive
     * metadata without leaking early-match information.
     *
     * @param first  first value
     * @param second second value
     * @return true when both values are equal
     */
    public static boolean constantTimeEquals(
            String first,
            String second
    ) {

        if (first == null || second == null) {
            return false;
        }

        byte[] firstBytes =
                first.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                );

        byte[] secondBytes =
                second.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                );

        return MessageDigest.isEqual(
                firstBytes,
                secondBytes
        );
    }

    /**
     * Checks whether the provided checksum matches the contents
     * of the specified file.
     *
     * @param filePath         path of file
     * @param expectedChecksum expected SHA-256 checksum
     * @return true when the checksum matches
     */
    public static boolean verifySha256(
            Path filePath,
            String expectedChecksum
    ) {

        if (expectedChecksum == null
                || expectedChecksum.isBlank()) {

            return false;
        }

        String actualChecksum =
                calculateSha256(filePath);

        return constantTimeEquals(
                actualChecksum.toLowerCase(),
                expectedChecksum.trim().toLowerCase()
        );
    }

    /**
     * Validates that a path points to a regular readable file.
     *
     * @param filePath file path
     */
    public static void validateReadableFile(
            Path filePath
    ) {

        if (filePath == null) {
            throw new EncryptionException(
                    "File path must not be null."
            );
        }

        if (!Files.exists(filePath)) {
            throw new EncryptionException(
                    "File does not exist: "
                            + filePath
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new EncryptionException(
                    "Path does not point to a regular file: "
                            + filePath
            );
        }

        if (!Files.isReadable(filePath)) {
            throw new EncryptionException(
                    "File is not readable: "
                            + filePath
            );
        }
    }

    /**
     * Safely deletes a file when it exists.
     *
     * This utility is useful for cleaning up temporary plain-text
     * or partially encrypted files after an operation fails.
     *
     * @param filePath path to delete
     */
    public static void deleteIfExists(
            Path filePath
    ) {

        if (filePath == null) {
            return;
        }

        try {
            Files.deleteIfExists(filePath);

        } catch (IOException exception) {
            throw new EncryptionException(
                    "Failed to delete file: "
                            + filePath,
                    exception
            );
        }
    }

    /**
     * Generates a new random AES-256 key and returns it as Base64.
     *
     * This method is intended for initial local setup or secret
     * generation. The resulting key should be stored in an
     * environment variable or secrets manager.
     *
     * @return Base64-encoded 256-bit AES key
     */
    public static String generateBase64Aes256Key() {

        byte[] keyBytes =
                new byte[AES_256_KEY_LENGTH_BYTES];

        SECURE_RANDOM.nextBytes(
                keyBytes
        );

        return encodeBase64(
                keyBytes
        );
    }

    private static MessageDigest createSha256Digest() {

        try {
            return MessageDigest.getInstance(
                    SHA_256_ALGORITHM
            );

        } catch (NoSuchAlgorithmException exception) {
            throw new EncryptionException(
                    "SHA-256 algorithm is not available.",
                    exception
            );
        }
    }
}