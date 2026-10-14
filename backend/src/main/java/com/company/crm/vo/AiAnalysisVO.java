package com.company.crm.vo;

import com.company.crm.dto.statistics.CustomerStatisticsResult;
import com.company.crm.enums.CustomerAnalysisIntent;
import com.company.crm.enums.StatisticsTimeRange;
import java.time.ZonedDateTime;

public record AiAnalysisVO(String answer, CustomerAnalysisIntent intent,
                           Metrics metrics, AnalysisContext analysisContext) {
    public record Metrics(long totalCustomers, long levelA, long levelB, long levelC,
                          long levelD, long unrated) {}
    public record AnalysisContext(String dataScope, StatisticsTimeRange timeRange,
                                  ZonedDateTime startTime, ZonedDateTime endTime,
                                  ZonedDateTime referenceTime, String timezone) {}

    public static AiAnalysisVO from(CustomerAnalysisIntent intent, CustomerStatisticsResult result) {
        String period = switch (result.timeRange()) {
            case ALL -> "全部时间客户";
            case CURRENT_MONTH -> "本月新增客户";
            case LAST_MONTH -> "上月新增客户";
            case RECENT_30_DAYS -> "近30天新增客户";
        };
        String answer = period + "共 " + result.totalCustomers() + " 个";
        if (intent == CustomerAnalysisIntent.CUSTOMER_GRADE_DISTRIBUTION) {
            answer += "，其中 A 级 " + result.levelA() + " 个、B 级 " + result.levelB()
                    + " 个、C 级 " + result.levelC() + " 个、D 级 " + result.levelD()
                    + " 个、未评级 " + result.unrated() + " 个";
        }
        return new AiAnalysisVO(answer + "。", intent,
                new Metrics(result.totalCustomers(), result.levelA(), result.levelB(), result.levelC(),
                        result.levelD(), result.unrated()),
                new AnalysisContext(result.dataScope(), result.timeRange(), result.startTime(), result.endTime(),
                        result.referenceTime(), result.timezone()));
    }
}
