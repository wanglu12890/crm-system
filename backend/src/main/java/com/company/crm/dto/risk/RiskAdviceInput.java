package com.company.crm.dto.risk;

import java.util.List;

/** No real IDs, names, user question or authorization metadata enter the advice model. */
public record RiskAdviceInput(List<Fact> customers) {
    public RiskAdviceInput { customers = List.copyOf(customers); }
    public record Fact(String alias, String customerLevel, CustomerRiskResult.State state,
            CustomerRiskResult.Level riskLevel, CustomerRiskResult.History history,
            Long elapsedDays, Integer thresholdDays, List<String> reasonKeys) {
        public Fact { reasonKeys = List.copyOf(reasonKeys); }
    }
    public static RiskAdviceInput from(CustomerRiskPage page) {
        var facts = new java.util.ArrayList<Fact>();
        for (var row : page.items()) facts.add(new Fact("r" + (facts.size()+1), row.customerLevel(), row.state(),
                row.riskLevel(), row.history(), row.elapsedDays(), row.thresholdDays(), row.reasons()));
        return new RiskAdviceInput(facts);
    }
    @Override public String toString() { return "RiskAdviceInput[redacted]"; }
}
