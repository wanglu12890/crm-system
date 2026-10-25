package com.company.crm.dto.risk;

import java.util.List;

public record RiskAdviceResult(List<Item> items) {
    public RiskAdviceResult { items = List.copyOf(items); }
    public enum Action {
        VERIFY_CURRENT_NEEDS("建议先核实客户当前需求，再由销售人员决定联系安排。"),
        CONFIRM_CONTACT_PREFERENCE("建议确认客户方便的联系渠道与时间，由销售人员人工安排跟进。"),
        DISCUSS_NEXT_STEP("建议沟通下一步事项，并在人工确认后安排后续跟进。"),
        VERIFY_DATA("建议先核验客户建档及跟进时间数据，再判断后续跟进安排。");
        private final String text;
        Action(String text) { this.text=text; }
        public String text() { return text; }
    }
    public record Item(String alias, List<Action> actions, List<String> reasonKeys) {
        public Item { actions=List.copyOf(actions); reasonKeys=List.copyOf(reasonKeys); }
    }
    /** Public advice item; identity is assigned by Java, never taken from model output. */
    public record RenderedItem(String customerId, List<String> actions, List<String> reasonKeys, String suggestion) {}
    @Override public String toString() { return "RiskAdviceResult[redacted]"; }
}
