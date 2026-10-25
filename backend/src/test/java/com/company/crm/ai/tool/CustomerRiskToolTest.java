package com.company.crm.ai.tool;

import com.company.crm.dto.risk.*;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.*;
import com.company.crm.security.datascope.*;
import com.company.crm.service.CustomerRiskService;
import org.junit.jupiter.api.Test;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerRiskToolTest {
    final CustomerRiskService risks = mock(CustomerRiskService.class);
    final CustomerRiskToolFactory factory = new CustomerRiskToolFactory(risks);
    final CustomerRiskQuery query = CustomerRiskQuery.priority();
    CustomerRiskAccessContext context(long id) {
        return new CustomerRiskAccessContext(id,"SALES_STAFF",20L,"SELF",CustomerQueryScopeIntent.CURRENT_AUTHORIZED,
                new DataScopeContext(DataScopeType.SELF,id,null));
    }
    CustomerRiskPage page(long id) {
        return new CustomerRiskPage(List.of(),0,1,20,"SELF","test",ZonedDateTime.now(),context(id));
    }
    @Test void successSingleUseAndFactoryIsolation() {
        var expected=context(7);
        var one=factory.create(7L,query,expected);
        var two=factory.create(8L,query,context(8));
        when(risks.analyze(7L,query,expected)).thenReturn(page(7));
        when(risks.analyze(eq(8L),eq(query),any())).thenReturn(page(8));
        assertThat(one).isNotSameAs(two);
        assertThat(one.getCustomerRiskAnalysis().accessContext().userId()).isEqualTo(7L);
        assertThat(two.getCustomerRiskAnalysis().accessContext().userId()).isEqualTo(8L);
        assertThatThrownBy(one::getCustomerRiskAnalysis).isInstanceOf(AiToolInvocationException.class);
        verify(risks,times(1)).analyze(7L,query,expected);
        assertThat(one.getState()).isEqualTo(CustomerRiskTool.State.SUCCEEDED);
    }
    @Test void failedFirstExecutionCannotRetry() {
        var expected=context(7); var tool=factory.create(7L,query,expected);
        when(risks.analyze(7L,query,expected)).thenThrow(new CustomerRiskException(CustomerRiskException.Code.NOT_FOUND));
        assertThatThrownBy(tool::getCustomerRiskAnalysis).isInstanceOf(CustomerRiskException.class);
        assertThatThrownBy(tool::getCustomerRiskAnalysis).isInstanceOf(AiToolInvocationException.class);
        assertThat(tool.getState()).isEqualTo(CustomerRiskTool.State.FAILED);
        verify(risks).analyze(7L,query,expected);
    }
    @Test void concurrentCallsEnterCoreOnlyOnce() throws Exception {
        var expected=context(7); var tool=factory.create(7L,query,expected);
        var entered=new CountDownLatch(1); var release=new CountDownLatch(1);
        when(risks.analyze(7L,query,expected)).thenAnswer(i->{ entered.countDown();
            if (!release.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("test timeout"); return page(7); });
        var executor=Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(tool::getCustomerRiskAnalysis);
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            var second=executor.submit(tool::getCustomerRiskAnalysis);
            assertThatThrownBy(()->second.get(5,TimeUnit.SECONDS)).hasCauseInstanceOf(AiToolInvocationException.class);
            release.countDown(); assertThat(first.get(5,TimeUnit.SECONDS)).isNotNull();
            verify(risks).analyze(7L,query,expected);
        } finally { release.countDown(); executor.shutdownNow(); }
    }
    @Test void foreignIdentityOrScopeNeverCreatesTool() {
        assertThatThrownBy(()->factory.create(8L,query,context(7))).isInstanceOf(CustomerRiskException.class);
        assertThatThrownBy(()->factory.create(7L,query,null)).isInstanceOf(CustomerRiskException.class);
        verifyNoInteractions(risks);
    }
}
