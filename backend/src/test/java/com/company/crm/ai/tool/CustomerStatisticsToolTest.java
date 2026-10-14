package com.company.crm.ai.tool;

import com.company.crm.dto.statistics.CustomerStatisticsResult;
import com.company.crm.enums.StatisticsTimeRange;
import com.company.crm.exception.AiAuthenticationContextException;
import com.company.crm.exception.AiToolArgumentException;
import com.company.crm.exception.AiToolInvocationException;
import com.company.crm.exception.CustomerStatisticsDataException;
import com.company.crm.exception.DataScopeConfigurationException;
import com.company.crm.service.CustomerStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerStatisticsToolTest {

    private final CustomerStatisticsService statisticsService = mock(CustomerStatisticsService.class);
    private final CustomerStatisticsToolFactory factory = new CustomerStatisticsToolFactory(statisticsService);

    @Test
    void shouldExposeOnlyWhitelistedTimeRangeInToolSchema() {
        CustomerStatisticsTool tool = factory.create(7L);
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder().toolObjects(tool).build().getToolCallbacks();

        assertThat(callbacks).hasSize(1);
        String schema = callbacks[0].getToolDefinition().inputSchema();
        assertThat(schema).contains("timeRange", "ALL", "CURRENT_MONTH", "LAST_MONTH", "RECENT_30_DAYS");
        assertThat(schema).doesNotContain("userId", "authenticatedUserId", "deptId", "dataScope",
                "role", "permission", "sql", "startTime", "endTime", "customerId", "salesUserId");
    }

    @Test
    void shouldUseBoundTrustedUserAndCaptureSuccessfulResult() {
        CustomerStatisticsResult expected = result("SELF", StatisticsTimeRange.CURRENT_MONTH);
        when(statisticsService.getStatistics(7L, StatisticsTimeRange.CURRENT_MONTH)).thenReturn(expected);
        CustomerStatisticsTool tool = factory.create(7L);

        CustomerStatisticsResult actual = tool.getCustomerStatistics(StatisticsTimeRange.CURRENT_MONTH);

        assertThat(actual).isSameAs(expected);
        assertThat(tool.getExecution().status()).isEqualTo(CustomerStatisticsTool.ExecutionStatus.SUCCEEDED);
        assertThat(tool.getExecution().result()).isSameAs(expected);
        verify(statisticsService).getStatistics(7L, StatisticsTimeRange.CURRENT_MONTH);
    }

    @Test
    void shouldKeepDifferentRequestToolsIsolated() {
        when(statisticsService.getStatistics(7L, StatisticsTimeRange.ALL)).thenReturn(result("SELF", StatisticsTimeRange.ALL));
        when(statisticsService.getStatistics(8L, StatisticsTimeRange.LAST_MONTH)).thenReturn(result("DEPT", StatisticsTimeRange.LAST_MONTH));
        CustomerStatisticsTool first = factory.create(7L);
        CustomerStatisticsTool second = factory.create(8L);

        first.getCustomerStatistics(StatisticsTimeRange.ALL);
        second.getCustomerStatistics(StatisticsTimeRange.LAST_MONTH);

        verify(statisticsService).getStatistics(7L, StatisticsTimeRange.ALL);
        verify(statisticsService).getStatistics(8L, StatisticsTimeRange.LAST_MONTH);
        assertThat(first.getExecution().result().dataScope()).isEqualTo("SELF");
        assertThat(second.getExecution().result().dataScope()).isEqualTo("DEPT");
    }

    @Test
    void shouldReportNotExecutedBeforeInvocation() {
        CustomerStatisticsTool tool = factory.create(7L);
        assertThat(tool.getExecution().status()).isEqualTo(CustomerStatisticsTool.ExecutionStatus.NOT_EXECUTED);
        assertThat(tool.getExecution().result()).isNull();
    }

    @Test
    void shouldRejectMissingTimeRangeWithoutCallingStatistics() {
        CustomerStatisticsTool tool = factory.create(7L);
        assertThatThrownBy(() -> tool.getCustomerStatistics(null)).isInstanceOf(AiToolArgumentException.class);
        assertThat(tool.getExecution().status()).isEqualTo(CustomerStatisticsTool.ExecutionStatus.FAILED);
        assertThat(tool.getExecution().result()).isNull();
        verify(statisticsService, never()).getStatistics(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPreserveScopeAndDataFailuresWithoutFalseSuccess() {
        DataScopeConfigurationException scopeFailure = new DataScopeConfigurationException("forbidden");
        when(statisticsService.getStatistics(7L, StatisticsTimeRange.ALL)).thenThrow(scopeFailure);
        CustomerStatisticsTool scopeTool = factory.create(7L);
        assertThatThrownBy(() -> scopeTool.getCustomerStatistics(StatisticsTimeRange.ALL)).isSameAs(scopeFailure);
        assertThat(scopeTool.getExecution().status()).isEqualTo(CustomerStatisticsTool.ExecutionStatus.FAILED);
        assertThat(scopeTool.getExecution().result()).isNull();

        CustomerStatisticsDataException dataFailure = new CustomerStatisticsDataException("invalid aggregate");
        when(statisticsService.getStatistics(8L, StatisticsTimeRange.ALL)).thenThrow(dataFailure);
        CustomerStatisticsTool dataTool = factory.create(8L);
        assertThatThrownBy(() -> dataTool.getCustomerStatistics(StatisticsTimeRange.ALL)).isSameAs(dataFailure);
        assertThat(dataTool.getExecution().status()).isEqualTo(CustomerStatisticsTool.ExecutionStatus.FAILED);
        assertThat(dataTool.getExecution().result()).isNull();
    }

    @Test
    void shouldRejectRepeatedInvocationAndKeepFirstSuccess() {
        CustomerStatisticsResult expected = result("ALL", StatisticsTimeRange.ALL);
        when(statisticsService.getStatistics(7L, StatisticsTimeRange.ALL)).thenReturn(expected);
        CustomerStatisticsTool tool = factory.create(7L);
        tool.getCustomerStatistics(StatisticsTimeRange.ALL);

        assertThatThrownBy(() -> tool.getCustomerStatistics(StatisticsTimeRange.ALL))
                .isInstanceOf(AiToolInvocationException.class);
        assertThat(tool.getExecution().status()).isEqualTo(CustomerStatisticsTool.ExecutionStatus.SUCCEEDED);
        assertThat(tool.getExecution().result()).isSameAs(expected);
        verify(statisticsService).getStatistics(7L, StatisticsTimeRange.ALL);
    }

    @Test
    void shouldRejectMissingOrInvalidTrustedIdentity() {
        assertThatThrownBy(() -> factory.create(null)).isInstanceOf(AiAuthenticationContextException.class);
        assertThatThrownBy(() -> factory.create(0L)).isInstanceOf(AiAuthenticationContextException.class);
    }

    private CustomerStatisticsResult result(String scope, StatisticsTimeRange range) {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 14, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai"));
        return new CustomerStatisticsResult(5, 1, 1, 1, 1, 1, scope, range,
                null, null, now, "Asia/Shanghai");
    }
}
