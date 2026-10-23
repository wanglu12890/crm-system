package com.company.crm.service;

import com.company.crm.dto.ai.CustomerRiskRequest;
import com.company.crm.vo.ai.RiskAnalysisVO;

public interface CustomerRiskAnalysisService {
    RiskAnalysisVO analyze(Long authenticatedUserId, CustomerRiskRequest request);
}
