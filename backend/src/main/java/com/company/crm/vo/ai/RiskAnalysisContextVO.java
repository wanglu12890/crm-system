package com.company.crm.vo.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RiskAnalysisContextVO(String authorizedScope, String requestedScope, String effectiveScope,
        OffsetDateTime analysisTime, String timezone, String ruleVersion, String consistency) {}
