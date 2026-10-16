package com.company.crm.ai.tool;

import com.company.crm.exception.AiAuthenticationContextException;
import com.company.crm.exception.AiToolArgumentException;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.service.CustomerStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerStatisticsToolFactory {
    private final CustomerStatisticsService customerStatisticsService;

    public CustomerStatisticsTool create(Long authenticatedUserId) {
        return create(authenticatedUserId, CustomerQueryScopeIntent.CURRENT_AUTHORIZED);
    }

    /** Scope is bound by Java orchestration, never supplied through the model-visible tool schema. */
    public CustomerStatisticsTool create(Long authenticatedUserId, CustomerQueryScopeIntent validatedRequestedScope) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new AiAuthenticationContextException();
        }
        if (validatedRequestedScope == null || validatedRequestedScope == CustomerQueryScopeIntent.UNSUPPORTED) {
            throw new AiToolArgumentException("客户统计查询范围不合法");
        }
        return new CustomerStatisticsTool(authenticatedUserId, customerStatisticsService, validatedRequestedScope);
    }
}
