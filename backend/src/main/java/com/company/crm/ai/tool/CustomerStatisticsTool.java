package com.company.crm.ai.tool;

import com.company.crm.dto.statistics.CustomerStatisticsResult;
import com.company.crm.enums.StatisticsTimeRange;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.AiToolArgumentException;
import com.company.crm.exception.AiToolInvocationException;
import com.company.crm.service.CustomerStatisticsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Request-isolated, read-only adapter around the trusted customer statistics service. */
public final class CustomerStatisticsTool {
    private static final ToolExecution NOT_EXECUTED = new ToolExecution(ExecutionStatus.NOT_EXECUTED, null);
    private static final ToolExecution RUNNING = new ToolExecution(ExecutionStatus.RUNNING, null);

    private final Long authenticatedUserId;
    private final CustomerStatisticsService customerStatisticsService;
    private final CustomerQueryScopeIntent validatedRequestedScope;
    private final AtomicReference<ToolExecution> execution = new AtomicReference<>(NOT_EXECUTED);

    CustomerStatisticsTool(Long authenticatedUserId, CustomerStatisticsService customerStatisticsService,
                           CustomerQueryScopeIntent validatedRequestedScope) {
        this.authenticatedUserId = Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        this.customerStatisticsService = Objects.requireNonNull(customerStatisticsService, "customerStatisticsService");
        this.validatedRequestedScope = Objects.requireNonNull(validatedRequestedScope, "validatedRequestedScope");
    }

    @Tool(name = "getCustomerStatistics",
            description = "统计当前登录用户有权查看的普通客户总量和A/B/C/D/未评级等级分布")
    public CustomerStatisticsResult getCustomerStatistics(
            @ToolParam(description = "时间范围，只允许 ALL、CURRENT_MONTH、LAST_MONTH、RECENT_30_DAYS")
            StatisticsTimeRange timeRange
    ) {
        if (!execution.compareAndSet(NOT_EXECUTED, RUNNING)) {
            throw new AiToolInvocationException("每次AI分析请求最多执行一次客户统计工具");
        }
        try {
            if (timeRange == null) {
                throw new AiToolArgumentException("客户统计时间范围不能为空");
            }
            CustomerStatisticsResult result = customerStatisticsService.getStatistics(
                    authenticatedUserId, timeRange, validatedRequestedScope);
            if (result == null) {
                throw new IllegalStateException("客户统计服务未返回结果");
            }
            execution.set(new ToolExecution(ExecutionStatus.SUCCEEDED, result));
            return result;
        } catch (RuntimeException exception) {
            execution.set(new ToolExecution(ExecutionStatus.FAILED, null));
            throw exception;
        }
    }

    public ToolExecution getExecution() {
        return execution.get();
    }

    public enum ExecutionStatus { NOT_EXECUTED, RUNNING, SUCCEEDED, FAILED }

    public record ToolExecution(ExecutionStatus status, CustomerStatisticsResult result) {
    }
}
