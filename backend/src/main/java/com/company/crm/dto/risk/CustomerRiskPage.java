package com.company.crm.dto.risk;

import java.time.ZonedDateTime;
import java.util.List;

public record CustomerRiskPage(List<CustomerRiskResult> items, long total, int page, int size,
        String actualScope, String ruleVersion, ZonedDateTime analysisTime, CustomerRiskAccessContext accessContext) {
    public CustomerRiskPage { items = List.copyOf(items); }
}
