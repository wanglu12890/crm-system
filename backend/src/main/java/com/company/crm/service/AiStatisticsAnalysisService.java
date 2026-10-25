package com.company.crm.service;

import com.company.crm.dto.ai.internal.AiAnalysisPlan;
import com.company.crm.vo.ai.AiAnalysisVO;

public interface AiStatisticsAnalysisService {
    AiAnalysisVO analyze(Long authenticatedUserId, AiAnalysisPlan.Statistics classification);
}
