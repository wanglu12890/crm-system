package com.company.crm.dto.risk;

import lombok.Data;
import java.time.LocalDateTime;

/** Minimal internal SQL projection, deliberately excluding contact and follow content. */
@Data
public class CustomerRiskAggregate {
    private Long id;
    private String customerName;
    private String customerNo;
    private String customerStatus;
    private String customerLevel;
    private LocalDateTime createdAt;
    private long historyCount;
    private long validCount;
    private long anomalousCount;
    private LocalDateTime lastValidFollowAt;
}
