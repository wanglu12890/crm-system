package com.company.crm.dto.ai.internal;

import com.company.crm.enums.CustomerAnalysisIntent;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.enums.StatisticsTimeRange;
import java.util.Objects;

/** Structurally validated semantics, NOT an authorized execution plan. */
public sealed interface AiAnalysisPlan {
    enum BusinessType { STATISTICS, RISK, UNSUPPORTED }
    enum RiskIntent { LIST, REASON, SUGGESTIONS }
    enum UnsupportedReason { OUT_OF_SCOPE, MIXED, AMBIGUOUS }
    BusinessType businessType();

    record Statistics(CustomerAnalysisIntent intent, StatisticsTimeRange timeRange,
            CustomerQueryScopeIntent queryScopeIntent) implements AiAnalysisPlan {
        public Statistics {
            Objects.requireNonNull(intent);
            Objects.requireNonNull(timeRange);
            Objects.requireNonNull(queryScopeIntent);
        }
        @Override public BusinessType businessType() { return BusinessType.STATISTICS; }
    }
    record Risk(RiskIntent riskIntent, CustomerQueryScopeIntent queryScopeIntent,
            com.company.crm.dto.risk.CustomerRiskQuery.Mode queryMode, java.util.Set<String> customerLevels) implements AiAnalysisPlan {
        public Risk(RiskIntent intent, CustomerQueryScopeIntent scope) {
            this(intent, scope, com.company.crm.dto.risk.CustomerRiskQuery.Mode.PRIORITY, java.util.Set.of());
        }
        public Risk {
            Objects.requireNonNull(riskIntent); Objects.requireNonNull(queryScopeIntent); Objects.requireNonNull(queryMode);
            customerLevels = java.util.Set.copyOf(customerLevels);
            if (!java.util.Set.of("A","B","C","D","UNRATED").containsAll(customerLevels)) throw new IllegalArgumentException();
        }
        @Override public BusinessType businessType() { return BusinessType.RISK; }
    }
    record Unsupported(UnsupportedReason reason) implements AiAnalysisPlan {
        public Unsupported { Objects.requireNonNull(reason); }
        @Override public BusinessType businessType() { return BusinessType.UNSUPPORTED; }
    }
}
