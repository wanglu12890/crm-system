package com.company.crm.service;

import com.company.crm.config.CustomerRiskProperties;
import com.company.crm.dto.risk.*;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.CustomerRiskException;
import com.company.crm.mapper.CustomerRiskMapper;
import com.company.crm.security.datascope.*;
import com.company.crm.service.impl.CustomerRiskServiceImpl;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CustomerRiskServiceTest {
    final CustomerRiskAccessPolicy access=mock(CustomerRiskAccessPolicy.class);
    final CustomerRiskMapper mapper=mock(CustomerRiskMapper.class);
    final CustomerRiskProperties properties=new CustomerRiskProperties();
    final Clock clock=Clock.fixed(Instant.parse("2026-10-18T04:00:00Z"),ZoneOffset.UTC);
    final DataScopeContext scope=new DataScopeContext(DataScopeType.SELF,7L,null);
    final CustomerRiskService service=new CustomerRiskServiceImpl(access,properties,mapper,new CustomerRiskEvaluator(),clock);
    @BeforeEach void setup() {when(access.resolve(eq(7L),any())).thenReturn(scope);}
    CustomerRiskAggregate row(long id,int days) {
        var c=new CustomerRiskAggregate();c.setId(id);c.setCustomerLevel("A");c.setCreatedAt(LocalDateTime.of(2026,10,18,12,0).minusDays(days));return c;
    }
    CustomerRiskQuery query(CustomerRiskQuery.Operation operation, CustomerRiskQuery.Mode mode, Long id,int page,int size) {
        return new CustomerRiskQuery(operation,mode,CustomerQueryScopeIntent.CURRENT_AUTHORIZED,Set.of(),id,page,size);
    }
    @Test void filterSortCountBeforePaginationAndFreezeTime() {
        when(mapper.selectCandidates(eq(scope),any(),any(),isNull(),eq(5001))).thenReturn(List.of(row(10,8),row(2,20),row(3,1),row(4,15)));
        var result=service.analyze(7L,query(CustomerRiskQuery.Operation.LIST,CustomerRiskQuery.Mode.PRIORITY,null,2,1));
        assertThat(result.total()).isEqualTo(3);assertThat(result.items()).extracting(CustomerRiskResult::customerId).containsExactly(4L);
        assertThat(result.actualScope()).isEqualTo("SELF");assertThat(result.analysisTime().getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
        verify(mapper).selectCandidates(scope,result.analysisTime().toLocalDateTime(),Set.of(),null,5001);
        verify(access,times(1)).resolve(7L,CustomerQueryScopeIntent.CURRENT_AUTHORIZED);
    }
    @Test void allStatesAndOffPageKeepTrueTotal() {
        when(mapper.selectCandidates(any(),any(),any(),isNull(),anyInt())).thenReturn(List.of(row(1,1),row(2,8)));
        var result=service.analyze(7L,query(CustomerRiskQuery.Operation.LIST,CustomerRiskQuery.Mode.ALL_STATES,null,Integer.MAX_VALUE,50));
        assertThat(result.total()).isEqualTo(2);assertThat(result.items()).isEmpty();
    }
    @Test void capacityRejectsWholeResponse() {
        properties.setMaxCandidates("1");when(mapper.selectCandidates(any(),any(),any(),isNull(),eq(2))).thenReturn(List.of(row(1,1),row(2,8)));
        assertThatThrownBy(()->service.analyze(7L,CustomerRiskQuery.priority())).isInstanceOf(CustomerRiskException.class)
            .extracting(e->((CustomerRiskException)e).getCode()).isEqualTo(CustomerRiskException.Code.CAPACITY_EXCEEDED);
    }
    @Test void reasonUsesAuthorizedIdAndDoesNotPriorityFilterLowOrSpecial() {
        when(mapper.selectCandidates(eq(scope),any(),any(),eq(44L),anyInt())).thenReturn(List.of(row(44,1)));
        var result=service.analyze(7L,query(CustomerRiskQuery.Operation.REASON,CustomerRiskQuery.Mode.PRIORITY,44L,1,20));
        assertThat(result.items()).hasSize(1);assertThat(result.items().get(0).state()).isEqualTo(CustomerRiskResult.State.OBSERVING);
        verify(mapper).selectCandidates(eq(scope),any(),any(),eq(44L),eq(5001));
    }
    @Test void nonexistentAndUnauthorizedShareSameFailure() {
        when(mapper.selectCandidates(any(),any(),any(),anyLong(),anyInt())).thenReturn(List.of());
        for(long id:List.of(44L,45L)) assertThatThrownBy(()->service.analyze(7L,query(CustomerRiskQuery.Operation.REASON,CustomerRiskQuery.Mode.PRIORITY,id,1,20)))
            .isInstanceOf(CustomerRiskException.class).extracting(e->((CustomerRiskException)e).getCode()).isEqualTo(CustomerRiskException.Code.NOT_FOUND);
    }
    @Test void duplicateCustomersAndInvalidQueriesFail() {
        when(mapper.selectCandidates(any(),any(),any(),isNull(),anyInt())).thenReturn(List.of(row(1,20),row(1,20)));
        assertThatThrownBy(()->service.analyze(7L,CustomerRiskQuery.priority())).isInstanceOf(CustomerRiskException.class);
        assertThatThrownBy(()->query(CustomerRiskQuery.Operation.LIST,CustomerRiskQuery.Mode.PRIORITY,null,1,51)).isInstanceOf(CustomerRiskException.class);
        assertThatThrownBy(()->query(CustomerRiskQuery.Operation.REASON,CustomerRiskQuery.Mode.PRIORITY,null,1,20)).isInstanceOf(CustomerRiskException.class);
    }
    @Test void transactionBoundaryIsShortReadOnlyAndIndependentOfCaller() throws Exception {
        var annotation=CustomerRiskServiceImpl.class.getMethod("analyze",Long.class,CustomerRiskQuery.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(annotation.readOnly()).isTrue();
        assertThat(annotation.isolation()).isEqualTo(org.springframework.transaction.annotation.Isolation.REPEATABLE_READ);
        assertThat(annotation.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }
    @Test void deniedAccessNeverReadsCustomers() {
        when(access.resolve(any(),any())).thenThrow(new CustomerRiskException(CustomerRiskException.Code.ACCESS_DENIED));
        assertThatThrownBy(()->service.analyze(7L,CustomerRiskQuery.priority())).isInstanceOf(CustomerRiskException.class);verifyNoInteractions(mapper);
    }
}
