package com.company.crm.service.impl;

import com.company.crm.dto.ai.CustomerRiskRequest;
import com.company.crm.exception.CustomerRiskException;
import com.company.crm.service.*;
import com.company.crm.vo.ai.RiskAnalysisVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerRiskAnalysisServiceImpl implements CustomerRiskAnalysisService {
    private final CustomerRiskService risks;
    private final CustomerRiskResponseAssembler assembler;

    @Override
    public RiskAnalysisVO analyze(Long userId, CustomerRiskRequest request) {
        if (userId == null || userId <= 0) throw new CustomerRiskException(CustomerRiskException.Code.AUTHENTICATION_REQUIRED);
        if (request == null) throw new CustomerRiskException(CustomerRiskException.Code.INVALID_QUERY);
        return assembler.assemble(userId, request, risks.analyze(userId, request.query()));
    }
}
