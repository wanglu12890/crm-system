package com.company.crm.dto.statistics;

import lombok.Data;

/** Persistence projection for the single-row customer statistics query. */
@Data
public class CustomerStatisticsAggregate {
    private Long totalCustomers;
    private Long levelA;
    private Long levelB;
    private Long levelC;
    private Long levelD;
    private Long unrated;
}
