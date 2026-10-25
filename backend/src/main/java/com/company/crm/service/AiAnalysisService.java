package com.company.crm.service;

import com.company.crm.vo.ai.AiAnalysisResponse;

public interface AiAnalysisService {

    AiAnalysisResponse analyze(Long authenticatedUserId, String question);
    AiAnalysisResponse analyze(Long authenticatedUserId, com.company.crm.dto.ai.AiAnalysisRequest request);
}
