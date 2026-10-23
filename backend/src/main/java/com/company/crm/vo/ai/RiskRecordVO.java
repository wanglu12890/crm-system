package com.company.crm.vo.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RiskRecordVO(String customerId, String customerNo, String customerName, String customerLevel,
        String customerStatus, String state, String riskLevel, String historyStatus, OffsetDateTime lastValidFollowAt,
        String anchorType, Long elapsedDays, Integer thresholdDays, Long excessDays,
        List<Reason> reasons, DataQuality dataQuality) {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Reason(String code, String message, Evidence evidence) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Evidence(OffsetDateTime anchorTime, Long elapsedDays, Integer thresholdDays, String levelGroup) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record DataQuality(String status, List<String> codes) {}
}
