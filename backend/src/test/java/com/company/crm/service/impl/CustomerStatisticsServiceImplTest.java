package com.company.crm.service.impl;

import com.company.crm.dto.statistics.CustomerStatisticsAggregate;
import com.company.crm.dto.statistics.CustomerStatisticsResult;
import com.company.crm.enums.StatisticsTimeRange;
import com.company.crm.exception.CustomerStatisticsDataException;
import com.company.crm.exception.DataScopeConfigurationException;
import com.company.crm.mapper.CustomerMapper;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.security.datascope.DataScopeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerStatisticsServiceImplTest {

    private static final long USER_ID = 10L;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-10T08:30:45Z");

    @Mock private CustomerMapper customerMapper;
    @Mock private DataScopeResolver dataScopeResolver;

    private CustomerStatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerStatisticsServiceImpl(
                customerMapper, dataScopeResolver, Clock.fixed(FIXED_INSTANT, SHANGHAI)
        );
    }

    @Test
    void shouldReturnCompleteStatisticsForAllScopeWithoutTimeBounds() {
        DataScopeContext scope = scope(DataScopeType.ALL, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull()))
                .thenReturn(aggregate(10, 1, 2, 3, 1, 3));

        CustomerStatisticsResult result = service.getStatistics(USER_ID, StatisticsTimeRange.ALL);

        assertThat(result.totalCustomers()).isEqualTo(10);
        assertThat(result.levelA()).isEqualTo(1);
        assertThat(result.levelB()).isEqualTo(2);
        assertThat(result.levelC()).isEqualTo(3);
        assertThat(result.levelD()).isEqualTo(1);
        assertThat(result.unrated()).isEqualTo(3);
        assertThat(result.dataScope()).isEqualTo("ALL");
        assertThat(result.startTime()).isNull();
        assertThat(result.endTime()).isNull();
        assertThat(result.referenceTime()).isEqualTo(ZonedDateTime.of(
                2026, 10, 10, 16, 30, 45, 0, SHANGHAI
        ));
        assertThat(result.timezone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    void shouldReturnAllZeroCategoriesForEmptyData() {
        DataScopeContext scope = scope(DataScopeType.ALL, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull()))
                .thenReturn(aggregate(0, null, null, null, null, null));

        CustomerStatisticsResult result = service.getStatistics(USER_ID, StatisticsTimeRange.ALL);

        assertThat(result.totalCustomers()).isZero();
        assertThat(result.levelA()).isZero();
        assertThat(result.levelB()).isZero();
        assertThat(result.levelC()).isZero();
        assertThat(result.levelD()).isZero();
        assertThat(result.unrated()).isZero();
    }

    @Test
    void shouldPassDepartmentScopeToMapperWithoutChangingIt() {
        DataScopeContext scope = scope(DataScopeType.DEPT, 20L);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull()))
                .thenReturn(aggregate(1, 1, 0, 0, 0, 0));

        CustomerStatisticsResult result = service.getStatistics(USER_ID, StatisticsTimeRange.ALL);

        assertThat(result.dataScope()).isEqualTo("DEPT");
        verify(customerMapper).selectCustomerStatistics(scope, null, null);
    }

    @Test
    void shouldPassSelfScopeToMapperWithoutChangingIt() {
        DataScopeContext scope = scope(DataScopeType.SELF, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull()))
                .thenReturn(aggregate(1, 0, 0, 0, 0, 1));

        CustomerStatisticsResult result = service.getStatistics(USER_ID, StatisticsTimeRange.ALL);

        assertThat(result.dataScope()).isEqualTo("SELF");
        verify(dataScopeResolver).resolve(USER_ID, "customer:list");
    }

    @Test
    void shouldCalculateCurrentMonthInShanghaiWithExclusiveEnd() {
        assertWindow(
                StatisticsTimeRange.CURRENT_MONTH,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0)
        );
    }

    @Test
    void shouldCalculateLastMonthInShanghaiWithExclusiveEnd() {
        assertWindow(
                StatisticsTimeRange.LAST_MONTH,
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 10, 1, 0, 0)
        );
    }

    @Test
    void shouldCalculateRecentThirtyDaysAsRollingDuration() {
        assertWindow(
                StatisticsTimeRange.RECENT_30_DAYS,
                LocalDateTime.of(2026, 9, 10, 16, 30, 45),
                LocalDateTime.of(2026, 10, 10, 16, 30, 45)
        );
    }

    @Test
    void shouldReuseWidestScopeAlreadyResolvedForMultipleRoles() {
        DataScopeContext scope = scope(DataScopeType.ALL, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull()))
                .thenReturn(aggregate(1, 1, 0, 0, 0, 0));

        CustomerStatisticsResult result = service.getStatistics(USER_ID, StatisticsTimeRange.ALL);

        assertThat(result.dataScope()).isEqualTo("ALL");
        verify(dataScopeResolver).resolve(USER_ID, "customer:list");
    }

    @Test
    void shouldPropagateMissingCustomerListPermissionFailure() {
        DataScopeConfigurationException failure =
                new DataScopeConfigurationException("no effective customer:list scope");
        assertResolverFailure(failure);
    }

    @Test
    void shouldPropagateMissingDepartmentFailure() {
        DataScopeConfigurationException failure =
                new DataScopeConfigurationException("DEPT scope requires a department");
        assertResolverFailure(failure);
    }

    @Test
    void shouldPropagateUnsupportedScopeFailure() {
        DataScopeConfigurationException failure =
                new DataScopeConfigurationException("unsupported data scope");
        assertResolverFailure(failure);
    }

    private void assertResolverFailure(DataScopeConfigurationException failure) {
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenThrow(failure);
        assertThatThrownBy(() -> service.getStatistics(USER_ID, StatisticsTimeRange.ALL))
                .isSameAs(failure);
        verifyNoInteractions(customerMapper);
    }

    @Test
    void shouldRejectAggregateContainingIllegalNonNullLevel() {
        DataScopeContext scope = scope(DataScopeType.ALL, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull()))
                .thenReturn(aggregate(6, 1, 1, 1, 1, 1));

        assertThatThrownBy(() -> service.getStatistics(USER_ID, StatisticsTimeRange.ALL))
                .isInstanceOf(CustomerStatisticsDataException.class)
                .hasMessageContaining("不一致");
    }

    @Test
    void shouldRejectMissingAggregateInsteadOfReturningZero() {
        DataScopeContext scope = scope(DataScopeType.ALL, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull())).thenReturn(null);

        assertThatThrownBy(() -> service.getStatistics(USER_ID, StatisticsTimeRange.ALL))
                .isInstanceOf(CustomerStatisticsDataException.class);
    }

    @Test
    void shouldPropagateMapperFailureInsteadOfReturningZero() {
        DataScopeContext scope = scope(DataScopeType.ALL, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
        when(customerMapper.selectCustomerStatistics(eq(scope), isNull(), isNull())).thenThrow(failure);

        assertThatThrownBy(() -> service.getStatistics(USER_ID, StatisticsTimeRange.ALL))
                .isSameAs(failure);
    }

    @Test
    void shouldRejectUntrustedNullArgumentsBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.getStatistics(null, StatisticsTimeRange.ALL))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.getStatistics(USER_ID, null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(dataScopeResolver, customerMapper);
    }

    private void assertWindow(StatisticsTimeRange range, LocalDateTime expectedStart, LocalDateTime expectedEnd) {
        DataScopeContext scope = scope(DataScopeType.ALL, null);
        when(dataScopeResolver.resolve(USER_ID, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerStatistics(eq(scope), any(), any()))
                .thenReturn(aggregate(0, 0, 0, 0, 0, 0));

        CustomerStatisticsResult result = service.getStatistics(USER_ID, range);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(customerMapper).selectCustomerStatistics(eq(scope), startCaptor.capture(), endCaptor.capture());
        assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
        assertThat(endCaptor.getValue()).isEqualTo(expectedEnd);
        assertThat(result.startTime().toLocalDateTime()).isEqualTo(expectedStart);
        assertThat(result.endTime().toLocalDateTime()).isEqualTo(expectedEnd);
    }

    private DataScopeContext scope(DataScopeType type, Long deptId) {
        return new DataScopeContext(type, USER_ID, deptId);
    }

    private CustomerStatisticsAggregate aggregate(
            long total, Integer levelA, Integer levelB, Integer levelC, Integer levelD, Integer unrated
    ) {
        CustomerStatisticsAggregate aggregate = new CustomerStatisticsAggregate();
        aggregate.setTotalCustomers(total);
        aggregate.setLevelA(levelA == null ? null : levelA.longValue());
        aggregate.setLevelB(levelB == null ? null : levelB.longValue());
        aggregate.setLevelC(levelC == null ? null : levelC.longValue());
        aggregate.setLevelD(levelD == null ? null : levelD.longValue());
        aggregate.setUnrated(unrated == null ? null : unrated.longValue());
        return aggregate;
    }
}
