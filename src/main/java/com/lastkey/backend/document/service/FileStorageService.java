package com.lastkey.backend.document.service;

import com.lastkey.backend.document.dto.storage.StorageResult;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StorageResult storeFile(
            MultipartFile file,
            String ownerFolder
    );

    Resource loadFileAsResource(
            String storedFileName,
            String ownerFolder
    );

    void deleteFile(
            String storedFileName,
            String ownerFolder
    );

    String getMimeType(
            MultipartFile file
    );

    String calculateChecksum(
            MultipartFile file
    );
}