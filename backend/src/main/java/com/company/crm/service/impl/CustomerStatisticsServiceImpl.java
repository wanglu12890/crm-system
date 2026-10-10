package com.company.crm.service.impl;

import com.company.crm.config.TimeConfig;
import com.company.crm.dto.statistics.CustomerStatisticsAggregate;
import com.company.crm.dto.statistics.CustomerStatisticsResult;
import com.company.crm.enums.StatisticsTimeRange;
import com.company.crm.exception.CustomerStatisticsDataException;
import com.company.crm.mapper.CustomerMapper;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.service.CustomerStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerStatisticsServiceImpl implements CustomerStatisticsService {
    private static final String CUSTOMER_LIST_PERMISSION = "customer:list";

    private final CustomerMapper customerMapper;
    private final DataScopeResolver dataScopeResolver;
    private final Clock businessClock;

    @Override
    @Transactional(readOnly = true)
    public CustomerStatisticsResult getStatistics(Long authenticatedUserId, StatisticsTimeRange timeRange) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
        Objects.requireNonNull(timeRange, "timeRange must not be null");

        DataScopeContext dataScope = dataScopeResolver.resolve(authenticatedUserId, CUSTOMER_LIST_PERMISSION);
        ZonedDateTime referenceTime = ZonedDateTime.now(businessClock)
                .withZoneSameInstant(TimeConfig.BUSINESS_ZONE);
        TimeWindow window = resolveTimeWindow(timeRange, referenceTime);
        CustomerStatisticsAggregate aggregate = customerMapper.selectCustomerStatistics(
                dataScope, toDatabaseTime(window.startTime()), toDatabaseTime(window.endTime())
        );
        if (aggregate == null) {
            throw new CustomerStatisticsDataException("客户统计查询未返回聚合结果");
        }

        long total = value(aggregate.getTotalCustomers());
        long levelA = value(aggregate.getLevelA());
        long levelB = value(aggregate.getLevelB());
        long levelC = value(aggregate.getLevelC());
        long levelD = value(aggregate.getLevelD());
        long unrated = value(aggregate.getUnrated());
        long classifiedTotal;
        try {
            classifiedTotal = Math.addExact(
                    Math.addExact(Math.addExact(levelA, levelB), Math.addExact(levelC, levelD)), unrated
            );
        } catch (ArithmeticException exception) {
            throw new CustomerStatisticsDataException("客户等级统计数量溢出");
        }
        if (total != classifiedTotal) {
            // Illegal non-null levels must not be silently disguised as "unrated".
            throw new CustomerStatisticsDataException("客户等级统计与客户总量不一致");
        }

        return new CustomerStatisticsResult(
                total, levelA, levelB, levelC, levelD, unrated,
                dataScope.getScopeCode(), timeRange,
                window.startTime(), window.endTime(), referenceTime,
                TimeConfig.BUSINESS_ZONE.getId()
        );
    }

    private TimeWindow resolveTimeWindow(StatisticsTimeRange timeRange, ZonedDateTime referenceTime) {
        return switch (timeRange) {
            case ALL -> new TimeWindow(null, null);
            case CURRENT_MONTH -> {
                ZonedDateTime start = referenceTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                yield new TimeWindow(start, start.plusMonths(1));
            }
            case LAST_MONTH -> {
                ZonedDateTime end = referenceTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                yield new TimeWindow(end.minusMonths(1), end);
            }
            case RECENT_30_DAYS -> new TimeWindow(referenceTime.minusDays(30), referenceTime);
        };
    }

    private LocalDateTime toDatabaseTime(ZonedDateTime value) {
        // MySQL DATETIME has no zone; application and JDBC interpret it using the frozen business zone.
        return value == null ? null : value.withZoneSameInstant(TimeConfig.BUSINESS_ZONE).toLocalDateTime();
    }

    private long value(Long value) {
        if (value == null) {
            return 0L;
        }
        if (value < 0) {
            throw new CustomerStatisticsDataException("客户统计数量不能为负数");
        }
        return value;
    }

    private record TimeWindow(ZonedDateTime startTime, ZonedDateTime endTime) {
    }
}
