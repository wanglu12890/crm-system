package com.company.crm.service;

import com.company.crm.vo.AiAnalysisVO;

public interface AiAnalysisService {

    AiAnalysisVO analyze(Long authenticatedUserId, String question);
}
