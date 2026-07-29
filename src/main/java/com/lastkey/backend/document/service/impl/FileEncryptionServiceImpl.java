package com.lastkey.backend.document.service.impl;

import com.lastkey.backend.document.service.FileEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class FileEncryptionServiceImpl
        implements FileEncryptionService {

    private static final String TRANSFORMATION =
            "AES/GCM/NoPadding";

    private static final String ALGORITHM =
            "AES";

    private static final int GCM_TAG_LENGTH_BITS =
            128;

    private static final int IV_LENGTH_BYTES =
            12;

    private static final int BUFFER_SIZE =
            8192;

    private final SecureRandom secureRandom;

    private final SecretKeySpec secretKey;

    public FileEncryptionServiceImpl(
            @Value(
                    "${lastkey.encryption.key-base64:"
                            + "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=}"
            )
            String base64EncryptionKey
    ) {

        this.secureRandom =
                new SecureRandom();

        byte[] encryptionKey =
                decodeAndValidateKey(
                        base64EncryptionKey
                );

        this.secretKey =
                new SecretKeySpec(
                        encryptionKey,
                        ALGORITHM
                );
    }

    @Override
    public void encrypt(
            InputStream plaintextInputStream,
            OutputStream encryptedOutputStream
    ) {

        if (plaintextInputStream == null) {
            throw new IllegalArgumentException(
                    "Plaintext input stream is required"
            );
        }

        if (encryptedOutputStream == null) {
            throw new IllegalArgumentException(
                    "Encrypted output stream is required"
            );
        }

        try {

            byte[] initializationVector =
                    generateInitializationVector();

            /*
             * Save the IV at the beginning of the encrypted file.
             * The decrypt method reads these first 12 bytes.
             */
            encryptedOutputStream.write(
                    initializationVector
            );

            Cipher cipher =
                    createCipher(
                            Cipher.ENCRYPT_MODE,
                            initializationVector
                    );

            byte[] inputBuffer =
                    new byte[BUFFER_SIZE];

            int bytesRead;

            while (
                    (
                            bytesRead =
                                    plaintextInputStream.read(
                                            inputBuffer
                                    )
                    ) != -1
            ) {

                byte[] encryptedChunk =
                        cipher.update(
                                inputBuffer,
                                0,
                                bytesRead
                        );

                if (
                        encryptedChunk != null
                                && encryptedChunk.length > 0
                ) {
                    encryptedOutputStream.write(
                            encryptedChunk
                    );
                }
            }

            /*
             * doFinal() generates the remaining encrypted bytes
             * and the AES-GCM authentication tag.
             */
            byte[] finalEncryptedBytes =
                    cipher.doFinal();

            if (
                    finalEncryptedBytes != null
                            && finalEncryptedBytes.length > 0
            ) {
                encryptedOutputStream.write(
                        finalEncryptedBytes
                );
            }

            encryptedOutputStream.flush();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not read or write file during encryption",
                    exception
            );

        } catch (GeneralSecurityException exception) {

            throw new IllegalStateException(
                    "File encryption failed",
                    exception
            );
        }
    }

    @Override
    public byte[] decrypt(
            InputStream encryptedInputStream
    ) {

        if (encryptedInputStream == null) {
            throw new IllegalArgumentException(
                    "Encrypted input stream is required"
            );
        }

        try {

            byte[] initializationVector =
                    encryptedInputStream.readNBytes(
                            IV_LENGTH_BYTES
                    );

            if (
                    initializationVector.length
                            != IV_LENGTH_BYTES
            ) {
                throw new IllegalStateException(
                        "Encrypted file does not contain a valid initialization vector"
                );
            }

            Cipher cipher =
                    createCipher(
                            Cipher.DECRYPT_MODE,
                            initializationVector
                    );

            try (
                    ByteArrayOutputStream decryptedOutputStream =
                            new ByteArrayOutputStream()
            ) {

                byte[] encryptedBuffer =
                        new byte[BUFFER_SIZE];

                int bytesRead;

                while (
                        (
                                bytesRead =
                                        encryptedInputStream.read(
                                                encryptedBuffer
                                        )
                        ) != -1
                ) {

                    byte[] decryptedChunk =
                            cipher.update(
                                    encryptedBuffer,
                                    0,
                                    bytesRead
                            );

                    if (
                            decryptedChunk != null
                                    && decryptedChunk.length > 0
                    ) {
                        decryptedOutputStream.write(
                                decryptedChunk
                        );
                    }
                }

                /*
                 * doFinal verifies the AES-GCM authentication tag.
                 * It fails if the file or key has been modified.
                 */
                byte[] finalDecryptedBytes =
                        cipher.doFinal();

                if (
                        finalDecryptedBytes != null
                                && finalDecryptedBytes.length > 0
                ) {
                    decryptedOutputStream.write(
                            finalDecryptedBytes
                    );
                }

                return decryptedOutputStream
                        .toByteArray();
            }

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not read encrypted file",
                    exception
            );

        } catch (GeneralSecurityException exception) {

            throw new IllegalStateException(
                    "File decryption failed. The file may be corrupted or the encryption key may be incorrect",
                    exception
            );
        }
    }

    private Cipher createCipher(
            int cipherMode,
            byte[] initializationVector
    ) throws GeneralSecurityException {

        Cipher cipher =
                Cipher.getInstance(
                        TRANSFORMATION
                );

        GCMParameterSpec parameterSpec =
                new GCMParameterSpec(
                        GCM_TAG_LENGTH_BITS,
                        initializationVector
                );

        cipher.init(
                cipherMode,
                secretKey,
                parameterSpec
        );

        return cipher;
    }

    private byte[] generateInitializationVector() {

        byte[] initializationVector =
                new byte[IV_LENGTH_BYTES];

        secureRandom.nextBytes(
                initializationVector
        );

        return initializationVector;
    }

    private byte[] decodeAndValidateKey(
            String base64EncryptionKey
    ) {

        if (
                base64EncryptionKey == null
                        || base64EncryptionKey.isBlank()
        ) {
            throw new IllegalStateException(
                    "Encryption key is missing"
            );
        }

        final byte[] decodedKey;

        try {
            decodedKey =
                    Base64.getDecoder()
                            .decode(
                                    base64EncryptionKey.trim()
                            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Encryption key must be valid Base64",
                    exception
            );
        }

        int keyLength =
                decodedKey.length;

        if (
                keyLength != 16
                        && keyLength != 24
                        && keyLength != 32
        ) {
            throw new IllegalStateException(
                    "Invalid AES encryption key length: "
                            + keyLength
                            + " bytes. The key must be 16, 24 or 32 bytes"
            );
        }

        return decodedKey;
    }
}