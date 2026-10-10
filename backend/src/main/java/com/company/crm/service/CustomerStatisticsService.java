package com.company.crm.service;

import com.company.crm.dto.statistics.CustomerStatisticsResult;
import com.company.crm.enums.StatisticsTimeRange;

public interface CustomerStatisticsService {
    /**
     * Returns statistics visible to an authenticated user.
     * The user id must come from the trusted server-side authentication flow, never from a client or model argument.
     */
    CustomerStatisticsResult getStatistics(Long authenticatedUserId, StatisticsTimeRange timeRange);
}
