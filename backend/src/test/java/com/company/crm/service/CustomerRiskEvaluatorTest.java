package com.company.crm.service;

import com.company.crm.config.CustomerRiskProperties;
import com.company.crm.dto.risk.*;
import com.company.crm.exception.CustomerRiskException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static com.company.crm.dto.risk.CustomerRiskResult.*;

class CustomerRiskEvaluatorTest {
    final CustomerRiskEvaluator evaluator = new CustomerRiskEvaluator();
    final ZonedDateTime now = ZonedDateTime.parse("2026-10-18T12:00:00+08:00[Asia/Shanghai]");
    final CustomerRiskProperties properties = new CustomerRiskProperties();
    CustomerRiskAggregate row(String grade, boolean followed, long millis) {
        var c = new CustomerRiskAggregate(); c.setId(1L); c.setCustomerLevel(grade);
        c.setCreatedAt(now.toLocalDateTime().minusDays(100));
        if (followed) {c.setHistoryCount(1);c.setValidCount(1);c.setLastValidFollowAt(now.toLocalDateTime().minusNanos(millis*1000000));}
        else c.setCreatedAt(now.toLocalDateTime().minusNanos(millis*1000000));
        return c;
    }
    @ParameterizedTest
    @CsvSource({"A,14,LOW,MEDIUM", "A,30,MEDIUM,HIGH", "B,30,LOW,MEDIUM", "B,60,MEDIUM,HIGH", "C,30,LOW,MEDIUM", "C,60,MEDIUM,HIGH", "D,30,LOW,MEDIUM", "D,60,MEDIUM,HIGH", "NULL,30,LOW,MEDIUM", "NULL,60,MEDIUM,HIGH"})
    void exactFollowBoundaries(String grade,int days,Level before,Level at) {
        grade = grade.equals("NULL") ? null : grade;
        long boundary = Duration.ofDays(days).toMillis();
        assertThat(evaluator.evaluate(row(grade,true,boundary-1),now,properties.snapshot()).riskLevel()).isEqualTo(before);
        assertThat(evaluator.evaluate(row(grade,true,boundary),now,properties.snapshot()).riskLevel()).isEqualTo(at);
        assertThat(evaluator.evaluate(row(grade,true,boundary+1),now,properties.snapshot()).riskLevel()).isEqualTo(at);
    }
    @ParameterizedTest @ValueSource(ints={7,14})
    void neverBoundaries(int days) {
        long boundary=Duration.ofDays(days).toMillis();
        var before=evaluator.evaluate(row("A",false,boundary-1),now,properties.snapshot());
        assertThat(before.state()).isEqualTo(days==7 ? State.OBSERVING : State.NORMAL);
        assertThat(before.riskLevel()).isEqualTo(days==7 ? null : Level.MEDIUM);
        for(long delta:new long[]{0,1}) {
            var result=evaluator.evaluate(row("A",false,boundary+delta),now,properties.snapshot());
            assertThat(result.riskLevel()).isEqualTo(days==7 ? Level.MEDIUM : Level.HIGH);
            assertThat(result.excessDays()).isZero();
            assertThat(result.reasons()).containsExactly("NEVER_FOLLOWED");
        }
    }
    @Test void anomalousOnlyDoesNotBecomeNeverFollowed() {
        var c=row("A",false,Duration.ofDays(40).toMillis()); c.setHistoryCount(2);c.setAnomalousCount(2);
        var result=evaluator.evaluate(c,now,properties.snapshot());
        assertThat(result.state()).isEqualTo(State.DATA_REVIEW);assertThat(result.riskLevel()).isNull();
        assertThat(result.history()).isEqualTo(History.INVALID_ONLY);
    }
    @Test void mixedHistoryUsesValidAnchorAndSingleReason() {
        var c=row("A",true,Duration.ofDays(35).toMillis());c.setHistoryCount(3);c.setAnomalousCount(2);
        var result=evaluator.evaluate(c,now,properties.snapshot());
        assertThat(result.riskLevel()).isEqualTo(Level.HIGH);assertThat(result.excessDays()).isEqualTo(5);
        assertThat(result.reasons()).containsExactly("FOLLOW_GAP");assertThat(result.qualityHints()).containsExactly("ANOMALOUS_FOLLOW_TIME");
    }
    @Test void invalidCreationAndFutureAnchorAreNotClamped() {
        var c=row("A",false,-1);
        assertThat(evaluator.evaluate(c,now,properties.snapshot()).state()).isEqualTo(State.DATA_REVIEW);
        c=row("A",true,-1);
        assertThat(evaluator.evaluate(c,now,properties.snapshot()).elapsedDays()).isNull();
        c.setCreatedAt(null);
        assertThat(evaluator.evaluate(c,now,properties.snapshot()).state()).isEqualTo(State.DATA_REVIEW);
    }
    @Test void invalidGradeAndBrokenAggregatesFailSafely() {
        var c=row("WRONG",false,1);assertThat(evaluator.evaluate(c,now,properties.snapshot()).state()).isEqualTo(State.DATA_REVIEW);
        c.setValidCount(2);
        assertThatThrownBy(()->evaluator.evaluate(c,now,properties.snapshot())).isInstanceOf(CustomerRiskException.class);
    }
    @Test void stableOrderingUsesSeverityGradeExcessAndNumericIdThenSpecialStates() {
        var high=row("A",true,Duration.ofDays(40).toMillis());high.setId(10L);
        var second=row("A",true,Duration.ofDays(40).toMillis());second.setId(2L);
        var low=row("A",true,1);low.setId(3L);
        var observing=row("A",false,1);observing.setId(4L);
        var review=row("BAD",false,1);review.setId(5L);
        var medium=row("A",true,Duration.ofDays(20).toMillis());medium.setId(6L);
        var b=row("B",true,Duration.ofDays(99).toMillis());b.setId(7L);
        var excess=row("A",true,Duration.ofDays(41).toMillis());excess.setId(8L);
        var ordered=List.of(low,review,high,medium,b,observing,second,excess).stream()
            .map(c->evaluator.evaluate(c,now,properties.snapshot())).sorted(CustomerRiskEvaluator.ordering()).map(CustomerRiskResult::customerId).toList();
        assertThat(ordered).containsExactly(8L,2L,10L,7L,6L,3L,4L,5L);
    }
}
