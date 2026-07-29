package com.lastkey.backend.ai.classification.service;

import com.lastkey.backend.ai.classification.dto.DocumentClassificationRequest;
import com.lastkey.backend.ai.classification.dto.DocumentClassificationResponse;

public interface DocumentClassificationService {

    DocumentClassificationResponse classify(
            DocumentClassificationRequest request
    );
}