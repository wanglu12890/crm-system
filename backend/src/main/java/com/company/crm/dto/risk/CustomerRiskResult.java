package com.company.crm.dto.risk;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerRiskResult(Long customerId, String customerName, String customerLevel, String customerNo, String customerStatus,
        State state, Level riskLevel, History history, LocalDateTime lastValidFollowAt,
        String anchorType, LocalDateTime anchorTime, String assessmentSummary, Long elapsedDays, Integer thresholdDays, Long excessDays,
        List<String> reasons, List<String> qualityHints) {
    public enum State { NORMAL, OBSERVING, DATA_REVIEW }
    public enum Level { HIGH, MEDIUM, LOW }
    public enum History { FOLLOWED, NEVER_FOLLOWED, INVALID_ONLY }
    public CustomerRiskResult { reasons = List.copyOf(reasons); qualityHints = List.copyOf(qualityHints); }
}
