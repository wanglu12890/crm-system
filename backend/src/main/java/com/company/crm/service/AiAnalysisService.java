package com.company.crm.service;

import com.company.crm.vo.ai.AiAnalysisVO;

public interface AiAnalysisService {

    AiAnalysisVO analyze(Long authenticatedUserId, String question);
}
