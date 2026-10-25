package com.company.crm.ai.tool;

import com.company.crm.dto.risk.*;
import com.company.crm.exception.CustomerRiskException;
import com.company.crm.service.CustomerRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import static com.company.crm.exception.CustomerRiskException.Code.*;

@Component
@RequiredArgsConstructor
public class CustomerRiskToolFactory {
    private final CustomerRiskService risks;
    public CustomerRiskTool create(Long userId, CustomerRiskQuery query, CustomerRiskAccessContext context) {
        if (userId == null || userId <= 0 || query == null || context == null
                || !userId.equals(context.userId()) || query.scope() != context.requestedScope()
                || context.effectiveScope() == null) throw new CustomerRiskException(ACCESS_CONTEXT_CHANGED);
        return new CustomerRiskTool(userId, query, context, risks);
    }
}
