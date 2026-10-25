package com.company.crm.vo.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RiskAnalysisVO(String status, String intent, String answer, RiskAnalysisContextVO analysisContext,
        NormalizedQuery normalizedQuery, Page page, List<RiskRecordVO> records, String resultFingerprint, Advice advice) implements AiAnalysisResponse {
    @Override public String toString() { return "RiskAnalysisVO[redacted]"; }
    public sealed interface NormalizedQuery permits ListQuery, ReasonQuery {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ListQuery(String scopeIntent, String queryMode, List<String> customerLevels,
            int page, int size) implements NormalizedQuery {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ReasonQuery(String scopeIntent, String customerId) implements NormalizedQuery {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Page(int number, int size, int returned, long total, long pages) {}
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Advice(String status, String source, String unavailableCode, String message, List<Object> items) {}
}
