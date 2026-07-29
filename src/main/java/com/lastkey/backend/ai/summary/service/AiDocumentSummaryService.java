package com.lastkey.backend.ai.summary.service;

import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryRequest;
import com.lastkey.backend.ai.summary.dto.AiDocumentSummaryResponse;

public interface AiDocumentSummaryService {

    AiDocumentSummaryResponse generateSummary(
            AiDocumentSummaryRequest request
    );
}