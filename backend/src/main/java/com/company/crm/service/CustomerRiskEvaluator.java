package com.company.crm.service;

import com.company.crm.dto.risk.*;
import com.company.crm.config.CustomerRiskProperties.RuleSnapshot;
import com.company.crm.exception.CustomerRiskException;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;
import static com.company.crm.dto.risk.CustomerRiskResult.*;
import static com.company.crm.config.TimeConfig.BUSINESS_ZONE;

@Component
public class CustomerRiskEvaluator {
    public CustomerRiskResult evaluate(CustomerRiskAggregate c, ZonedDateTime time, RuleSnapshot rule) {
        if (c.getId() == null || c.getHistoryCount() < 0 || c.getValidCount() < 0 || c.getAnomalousCount() < 0
                || c.getValidCount() > c.getHistoryCount() || c.getAnomalousCount() != c.getHistoryCount()-c.getValidCount()
                || (c.getValidCount() == 0) != (c.getLastValidFollowAt() == null))
            throw new CustomerRiskException(CustomerRiskException.Code.INVALID_DATA);
        History history = c.getValidCount() > 0 ? History.FOLLOWED : c.getHistoryCount() > 0 ? History.INVALID_ONLY : History.NEVER_FOLLOWED;
        List<String> quality = new ArrayList<>();
        if (c.getAnomalousCount() > 0) quality.add("ANOMALOUS_FOLLOW_TIME");
        if (c.getCreatedAt() == null || c.getCreatedAt().atZone(BUSINESS_ZONE).toInstant().isAfter(time.toInstant())) quality.add("INVALID_CREATED_AT");
        if (c.getCustomerLevel() != null && !List.of("A","B","C","D").contains(c.getCustomerLevel())) quality.add("INVALID_CUSTOMER_LEVEL");
        if (history == History.INVALID_ONLY || quality.contains("INVALID_CREATED_AT") || quality.contains("INVALID_CUSTOMER_LEVEL"))
            return result(c,State.DATA_REVIEW,null,history,null,null,null,List.of(),quality);
        LocalDateTime anchor = history == History.FOLLOWED ? c.getLastValidFollowAt() : c.getCreatedAt();
        Duration elapsed = Duration.between(anchor.atZone(BUSINESS_ZONE).toInstant(),time.toInstant());
        if (elapsed.isNegative() || anchor.isBefore(c.getCreatedAt())) {
            quality.add("INVALID_ANCHOR");
            return result(c,State.DATA_REVIEW,null,history,null,null,null,List.of(),quality);
        }
        int medium = history == History.NEVER_FOLLOWED ? rule.neverMediumDays() : "A".equals(c.getCustomerLevel()) ? rule.aMediumDays() : rule.otherMediumDays();
        int high = history == History.NEVER_FOLLOWED ? rule.neverHighDays() : "A".equals(c.getCustomerLevel()) ? rule.aHighDays() : rule.otherHighDays();
        if (history == History.NEVER_FOLLOWED && elapsed.compareTo(Duration.ofDays(medium)) < 0)
            return result(c,State.OBSERVING,null,history,elapsed.toDays(),medium,null,List.of(),quality);
        Level level = elapsed.compareTo(Duration.ofDays(high)) >= 0 ? Level.HIGH : elapsed.compareTo(Duration.ofDays(medium)) >= 0 ? Level.MEDIUM : Level.LOW;
        int threshold = level == Level.HIGH ? high : level == Level.MEDIUM ? medium : 0;
        return result(c,State.NORMAL,level,history,elapsed.toDays(),threshold,elapsed.toDays()-threshold,
                level == Level.LOW ? List.of() : List.of(history == History.FOLLOWED ? "FOLLOW_GAP" : "NEVER_FOLLOWED"),quality);
    }
    private CustomerRiskResult result(CustomerRiskAggregate c, State state, Level level, History history,
            Long elapsed,Integer threshold,Long excess,List<String> reasons,List<String> quality) {
        return new CustomerRiskResult(c.getId(),c.getCustomerName(),c.getCustomerLevel(),c.getCustomerNo(),c.getCustomerStatus(),state,level,history,c.getLastValidFollowAt(),
                state == State.DATA_REVIEW ? "NONE" : history == History.FOLLOWED ? "LAST_VALID_FOLLOW" : "CUSTOMER_CREATED", state == State.DATA_REVIEW ? null : history == History.FOLLOWED ? c.getLastValidFollowAt() : c.getCreatedAt(),
                state == State.DATA_REVIEW ? "Customer data requires review" : state == State.OBSERVING ? "New customer observation period"
                : level == Level.LOW ? "Follow interval below attention threshold" : history == History.FOLLOWED ? "Follow interval reached attention threshold" : "No follow history after attention threshold",
                elapsed,threshold,excess,reasons,quality);
    }
    public static Comparator<CustomerRiskResult> ordering() {
        return Comparator.comparing(CustomerRiskResult::state)
                .thenComparing(r -> r.riskLevel() == null ? 3 : r.riskLevel().ordinal())
                .thenComparingInt(r -> r.customerLevel() == null ? 4 : switch(r.customerLevel()) {case "A" -> 0; case "B" -> 1; case "C" -> 2; case "D" -> 3; default -> 4;})
                .thenComparing(CustomerRiskResult::excessDays,Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CustomerRiskResult::customerId);
    }
}
