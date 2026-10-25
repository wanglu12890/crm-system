package com.company.crm.service;

import com.company.crm.dto.risk.*;

public interface CustomerRiskService {
    CustomerRiskPage analyze(Long authenticatedUserId, CustomerRiskQuery query);
    CustomerRiskPage analyze(Long authenticatedUserId, CustomerRiskQuery query, CustomerRiskAccessContext expectedContext);
}
