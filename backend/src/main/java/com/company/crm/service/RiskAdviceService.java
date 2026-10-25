package com.company.crm.service;

import com.company.crm.dto.risk.*;

public interface RiskAdviceService {
    RiskAdviceResult generate(RiskAdviceInput input, long remainingMillis);
}
