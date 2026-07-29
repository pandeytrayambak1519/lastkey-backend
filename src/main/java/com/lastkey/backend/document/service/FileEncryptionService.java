package com.lastkey.backend.document.service;

import java.io.InputStream;
import java.io.OutputStream;

public interface FileEncryptionService {

    /**
     * Encrypts plaintext data using AES-256-GCM and writes
     * the encrypted result to the supplied output stream.
     */
    void encrypt(
            InputStream plaintextInputStream,
            OutputStream encryptedOutputStream
    );

    /**
     * Decrypts an AES-256-GCM encrypted file and returns
     * its original plaintext bytes.
     */
    byte[] decrypt(
            InputStream encryptedInputStream
    );
}