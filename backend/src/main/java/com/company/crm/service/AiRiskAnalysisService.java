package com.company.crm.service;

import com.company.crm.dto.ai.AiAnalysisRequest;
import com.company.crm.dto.ai.internal.AiAnalysisPlan;
import com.company.crm.vo.ai.RiskAnalysisVO;

public interface AiRiskAnalysisService {
    RiskAnalysisVO analyze(Long userId, AiAnalysisPlan.Risk candidate, AiAnalysisRequest request);
    default RiskAnalysisVO analyze(Long userId, AiAnalysisPlan.Risk candidate, AiAnalysisRequest request, long startedNanos) {
        return analyze(userId, candidate, request);
    }
}
