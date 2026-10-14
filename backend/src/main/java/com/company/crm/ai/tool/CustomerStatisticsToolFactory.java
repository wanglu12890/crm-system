package com.company.crm.ai.tool;

import com.company.crm.exception.AiAuthenticationContextException;
import com.company.crm.service.CustomerStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerStatisticsToolFactory {
    private final CustomerStatisticsService customerStatisticsService;

    public CustomerStatisticsTool create(Long authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            throw new AiAuthenticationContextException();
        }
        return new CustomerStatisticsTool(authenticatedUserId, customerStatisticsService);
    }
}
