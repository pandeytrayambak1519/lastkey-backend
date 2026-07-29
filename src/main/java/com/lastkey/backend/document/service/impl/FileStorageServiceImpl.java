package com.lastkey.backend.document.service.impl;

import com.lastkey.backend.config.FileStorageProperties;
import com.lastkey.backend.document.dto.storage.StorageResult;
import com.lastkey.backend.document.exception.FileStorageException;
import com.lastkey.backend.document.service.FileEncryptionService;
import com.lastkey.backend.document.service.FileStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl
        implements FileStorageService {

    private static final Set<String> ALLOWED_FILE_TYPES =
            Set.of(
                    "application/pdf",
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private static final Map<String, String> MIME_EXTENSIONS =
            Map.of(
                    "application/pdf",
                    ".pdf",

                    "image/jpeg",
                    ".jpg",

                    "image/png",
                    ".png",

                    "image/webp",
                    ".webp"
            );

    private static final long MAX_FILE_SIZE =
            20L * 1024 * 1024;

    private static final String ENCRYPTED_FILE_EXTENSION =
            ".lastkey";

    private final Path rootStorageLocation;

    private final FileEncryptionService
            fileEncryptionService;

    public FileStorageServiceImpl(
            FileStorageProperties properties,
            FileEncryptionService fileEncryptionService
    ) {

        this.fileEncryptionService =
                fileEncryptionService;

        String uploadDirectory =
                properties.getUploadDir();

        if (uploadDirectory == null
                || uploadDirectory.isBlank()) {

            throw new FileStorageException(
                    "Property 'file.upload-dir' is missing"
            );
        }

        this.rootStorageLocation =
                Paths.get(uploadDirectory)
                        .toAbsolutePath()
                        .normalize();

        try {

            Files.createDirectories(
                    rootStorageLocation
            );

        } catch (IOException exception) {

            throw new FileStorageException(
                    "Could not create file upload directory",
                    exception
            );
        }
    }

    @Override
    public StorageResult storeFile(
            MultipartFile file,
            String ownerFolder
    ) {

        validateFile(file);

        String mimeType =
                getMimeType(file);

        /*
         * Preserve the original extension inside the encrypted
         * stored filename for easier internal diagnostics.
         *
         * Example:
         * UUID.pdf.lastkey
         */
        String originalExtension =
                MIME_EXTENSIONS.get(
                        mimeType
                );

        String storedFileName =
                UUID.randomUUID()
                        + originalExtension
                        + ENCRYPTED_FILE_EXTENSION;

        Path userDirectory =
                createUserDirectory(
                        ownerFolder
                );

        Path targetLocation =
                userDirectory
                        .resolve(
                                storedFileName
                        )
                        .normalize();

        validateStoragePath(
                targetLocation,
                userDirectory
        );

        try (
                InputStream plaintextInputStream =
                        file.getInputStream();

                OutputStream encryptedOutputStream =
                        Files.newOutputStream(
                                targetLocation,
                                StandardOpenOption.CREATE_NEW,
                                StandardOpenOption.WRITE
                        )
        ) {

            fileEncryptionService.encrypt(
                    plaintextInputStream,
                    encryptedOutputStream
            );

            encryptedOutputStream.flush();

            String originalChecksum =
                    calculateChecksum(file);

            String encryptedChecksum =
                    calculateChecksum(targetLocation);

            return StorageResult.builder()
                    .storedFileName(storedFileName)
                    .encrypted(true)
                    .encryptionAlgorithm("AES/GCM/NoPadding")
                    .encryptionKeyVersion(1)
                    .initializationVector(null)
                    .originalChecksum(originalChecksum)
                    .encryptedChecksum(encryptedChecksum)
                    .originalSize(file.getSize())
                    .encryptedSize(Files.size(targetLocation))
                    .mimeType(mimeType)
                    .originalFileName(
                            sanitizeFileName(
                                    file.getOriginalFilename()
                            )
                    )
                    .build();

        } catch (IOException exception) {

            deletePartiallyStoredFile(
                    targetLocation
            );

            throw new FileStorageException(
                    "Could not store encrypted file: "
                            + sanitizeFileName(
                            file.getOriginalFilename()
                    ),
                    exception
            );

        } catch (RuntimeException exception) {

            deletePartiallyStoredFile(
                    targetLocation
            );

            throw exception;
        }
    }

    @Override
    public Resource loadFileAsResource(
            String storedFileName,
            String ownerFolder
    ) {

        validateStoredFileName(
                storedFileName
        );

        Path userDirectory =
                getUserDirectory(
                        ownerFolder
                );

        Path filePath =
                userDirectory
                        .resolve(
                                storedFileName
                        )
                        .normalize();

        validateStoragePath(
                filePath,
                userDirectory
        );

        if (!Files.exists(filePath)
                || !Files.isRegularFile(filePath)
                || !Files.isReadable(filePath)) {

            throw new FileStorageException(
                    "File not found or cannot be read"
            );
        }

        try (
                InputStream encryptedInputStream =
                        Files.newInputStream(
                                filePath,
                                StandardOpenOption.READ
                        )
        ) {

            byte[] decryptedFileBytes =
                    fileEncryptionService.decrypt(
                            encryptedInputStream
                    );

            /*
             * The decrypted bytes exist only in memory.
             * No temporary unencrypted file is written to disk.
             */
            return new NamedByteArrayResource(
                    decryptedFileBytes,
                    removeEncryptedExtension(
                            storedFileName
                    )
            );

        } catch (IOException exception) {

            throw new FileStorageException(
                    "Could not read encrypted document",
                    exception
            );
        }
    }

    @Override
    public void deleteFile(
            String storedFileName,
            String ownerFolder
    ) {

        validateStoredFileName(
                storedFileName
        );

        Path userDirectory =
                getUserDirectory(
                        ownerFolder
                );

        Path filePath =
                userDirectory
                        .resolve(
                                storedFileName
                        )
                        .normalize();

        validateStoragePath(
                filePath,
                userDirectory
        );

        try {

            Files.deleteIfExists(
                    filePath
            );

            deleteDirectoryIfEmpty(
                    userDirectory
            );

        } catch (IOException exception) {

            throw new FileStorageException(
                    "Could not delete stored file",
                    exception
            );
        }
    }

    @Override
    public String getMimeType(
            MultipartFile file
    ) {

        if (file == null
                || file.isEmpty()) {

            throw new FileStorageException(
                    "Cannot detect type of an empty file"
            );
        }

        try (
                InputStream inputStream =
                        file.getInputStream()
        ) {

            byte[] header =
                    inputStream.readNBytes(
                            12
                    );

            String detectedType =
                    detectMimeType(
                            header
                    );

            if (detectedType == null
                    || !ALLOWED_FILE_TYPES.contains(
                    detectedType
            )) {

                throw new FileStorageException(
                        "Only PDF, JPG, PNG and WEBP "
                                + "files are allowed"
                );
            }

            return detectedType;

        } catch (IOException exception) {

            throw new FileStorageException(
                    "Could not determine file type",
                    exception
            );
        }
    }

    @Override
    public String calculateChecksum(
            MultipartFile file
    ) {

        if (file == null
                || file.isEmpty()) {

            throw new FileStorageException(
                    "Cannot calculate checksum "
                            + "of an empty file"
            );
        }

        try (
                InputStream inputStream =
                        file.getInputStream()
        ) {

            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] buffer =
                    new byte[8192];

            int bytesRead;

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                messageDigest.update(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return HexFormat.of()
                    .formatHex(
                            messageDigest.digest()
                    );

        } catch (IOException exception) {

            throw new FileStorageException(
                    "Could not calculate file checksum",
                    exception
            );

        } catch (NoSuchAlgorithmException exception) {

            throw new FileStorageException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    private String calculateChecksum(
            Path filePath
    ) {

        try (
                InputStream inputStream =
                        Files.newInputStream(
                                filePath,
                                StandardOpenOption.READ
                        )
        ) {

            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] buffer =
                    new byte[8192];

            int bytesRead;

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                messageDigest.update(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return HexFormat.of()
                    .formatHex(
                            messageDigest.digest()
                    );

        } catch (IOException exception) {

            throw new FileStorageException(
                    "Could not calculate stored file checksum",
                    exception
            );

        } catch (NoSuchAlgorithmException exception) {

            throw new FileStorageException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    private void validateFile(
            MultipartFile file
    ) {

        if (file == null
                || file.isEmpty()) {

            throw new FileStorageException(
                    "Please select a document to upload"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new FileStorageException(
                    "File size cannot exceed 20 MB"
            );
        }

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new FileStorageException(
                    "Invalid file name"
            );
        }

        sanitizeFileName(
                originalFileName
        );

        /*
         * Validate using actual file bytes rather than trusting
         * the content type supplied by the browser.
         */
        getMimeType(file);
    }

    private Path createUserDirectory(
            String ownerFolder
    ) {

        Path userDirectory =
                getUserDirectory(
                        ownerFolder
                );

        try {

            Files.createDirectories(
                    userDirectory
            );

            return userDirectory;

        } catch (IOException exception) {

            throw new FileStorageException(
                    "Could not create user storage directory",
                    exception
            );
        }
    }

    private Path getUserDirectory(
            String ownerFolder
    ) {

        if (ownerFolder == null
                || ownerFolder.isBlank()) {

            throw new FileStorageException(
                    "Owner folder is required"
            );
        }

        String normalizedOwnerFolder =
                ownerFolder.trim();

        if (!normalizedOwnerFolder.matches(
                "[a-zA-Z0-9_-]+"
        )) {

            throw new FileStorageException(
                    "Invalid owner folder"
            );
        }

        Path userDirectory =
                rootStorageLocation
                        .resolve(
                                normalizedOwnerFolder
                        )
                        .normalize();

        if (!userDirectory.startsWith(
                rootStorageLocation
        )) {

            throw new FileStorageException(
                    "Invalid owner storage path"
            );
        }

        return userDirectory;
    }

    private void validateStoragePath(
            Path filePath,
            Path expectedDirectory
    ) {

        if (!filePath.startsWith(
                expectedDirectory
        )) {

            throw new FileStorageException(
                    "Invalid file storage path"
            );
        }
    }

    private void validateStoredFileName(
            String storedFileName
    ) {

        if (storedFileName == null
                || storedFileName.isBlank()) {

            throw new FileStorageException(
                    "Stored file name is required"
            );
        }

        String safeFileName =
                Paths.get(storedFileName)
                        .getFileName()
                        .toString();

        if (!safeFileName.equals(
                storedFileName
        )
                || storedFileName.contains("..")) {

            throw new FileStorageException(
                    "Invalid stored file name"
            );
        }
    }

    private String sanitizeFileName(
            String fileName
    ) {

        String sanitized =
                Paths.get(fileName)
                        .getFileName()
                        .toString()
                        .replaceAll(
                                "[^a-zA-Z0-9._-]",
                                "_"
                        );

        if (sanitized.isBlank()
                || sanitized.contains("..")) {

            throw new FileStorageException(
                    "Invalid file name"
            );
        }

        return sanitized;
    }

    private String detectMimeType(
            byte[] header
    ) {

        if (isPdf(header)) {
            return "application/pdf";
        }

        if (isJpeg(header)) {
            return "image/jpeg";
        }

        if (isPng(header)) {
            return "image/png";
        }

        if (isWebp(header)) {
            return "image/webp";
        }

        return null;
    }

    private boolean isPdf(
            byte[] header
    ) {

        return header.length >= 5
                && header[0] == '%'
                && header[1] == 'P'
                && header[2] == 'D'
                && header[3] == 'F'
                && header[4] == '-';
    }

    private boolean isJpeg(
            byte[] header
    ) {

        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(
            byte[] header
    ) {

        byte[] pngSignature = {
                (byte) 0x89,
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A
        };

        if (header.length
                < pngSignature.length) {

            return false;
        }

        for (
                int index = 0;
                index < pngSignature.length;
                index++
        ) {

            if (header[index]
                    != pngSignature[index]) {

                return false;
            }
        }

        return true;
    }

    private boolean isWebp(
            byte[] header
    ) {

        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private void deletePartiallyStoredFile(
            Path targetLocation
    ) {

        try {

            Files.deleteIfExists(
                    targetLocation
            );

        } catch (IOException ignored) {

            /*
             * Keep the original encryption or storage exception.
             */
        }
    }

    private void deleteDirectoryIfEmpty(
            Path directory
    ) {

        if (directory.equals(
                rootStorageLocation
        )) {
            return;
        }

        try (
                var entries =
                        Files.list(directory)
        ) {

            if (entries.findAny().isEmpty()) {
                Files.deleteIfExists(
                        directory
                );
            }

        } catch (IOException ignored) {

            /*
             * Failure to remove an empty owner directory must
             * not make document deletion fail.
             */
        }
    }

    private String removeEncryptedExtension(
            String storedFileName
    ) {

        if (storedFileName.endsWith(
                ENCRYPTED_FILE_EXTENSION
        )) {

            return storedFileName.substring(
                    0,
                    storedFileName.length()
                            - ENCRYPTED_FILE_EXTENSION.length()
            );
        }

        return storedFileName;
    }

    private static final class
    NamedByteArrayResource
            extends ByteArrayResource {

        private final String fileName;

        private NamedByteArrayResource(
                byte[] byteArray,
                String fileName
        ) {

            super(byteArray);

            this.fileName =
                    fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
