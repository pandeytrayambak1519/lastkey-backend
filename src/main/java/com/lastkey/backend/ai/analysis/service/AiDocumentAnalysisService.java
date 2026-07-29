package com.lastkey.backend.ai.analysis.service;

import com.lastkey.backend.ai.analysis.dto.AiDocumentAnalysisRequest;
import com.lastkey.backend.ai.analysis.dto.AiDocumentAnalysisResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AiDocumentAnalysisService {

    AiDocumentAnalysisResponse analyzeDocument(
            MultipartFile file,
            AiDocumentAnalysisRequest request
    );
}