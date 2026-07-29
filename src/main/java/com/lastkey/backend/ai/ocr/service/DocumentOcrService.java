package com.lastkey.backend.ai.ocr.service;

import com.lastkey.backend.ai.ocr.dto.DocumentOcrResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentOcrService {

    DocumentOcrResponse extractText(
            MultipartFile file
    );
}