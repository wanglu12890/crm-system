package com.company.crm.dto.statistics;

import com.company.crm.enums.StatisticsTimeRange;

import java.time.ZonedDateTime;

/** Immutable, model-independent result returned by the customer statistics domain service. */
public record CustomerStatisticsResult(
        long totalCustomers,
        long levelA,
        long levelB,
        long levelC,
        long levelD,
        long unrated,
        String dataScope,
        StatisticsTimeRange timeRange,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        ZonedDateTime referenceTime,
        String timezone
) {
}
