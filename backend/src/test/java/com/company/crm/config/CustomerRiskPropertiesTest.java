package com.company.crm.config;

import com.company.crm.exception.CustomerRiskException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import static org.assertj.core.api.Assertions.*;

class CustomerRiskPropertiesTest {
    @Configuration(proxyBeanMethods=false)
    @EnableConfigurationProperties(CustomerRiskProperties.class)
    static class TestConfig {
        @Bean com.company.crm.service.CustomerStatisticsService v1Statistics() {
            var mapper=org.mockito.Mockito.mock(com.company.crm.mapper.CustomerMapper.class);
            var resolver=org.mockito.Mockito.mock(com.company.crm.security.datascope.DataScopeResolver.class);
            var scope=new com.company.crm.security.datascope.DataScopeContext(com.company.crm.security.datascope.DataScopeType.SELF,7L,null);
            org.mockito.Mockito.when(resolver.resolve(7L,"customer:list")).thenReturn(scope);
            org.mockito.Mockito.when(mapper.selectCustomerStatistics(scope,null,null)).thenReturn(new com.company.crm.dto.statistics.CustomerStatisticsAggregate());
            return new com.company.crm.service.impl.CustomerStatisticsServiceImpl(mapper,resolver,java.time.Clock.systemUTC(),
                org.mockito.Mockito.mock(com.company.crm.service.CustomerAnalysisScopePolicy.class));
        }
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","30","not-a-number","2147483648",""})
    void badV2ConfigDoesNotAbortV1Startup(String value) {
        new ApplicationContextRunner().withUserConfiguration(TestConfig.class)
            .withPropertyValues("crm.ai.risk.rules.a-medium-days="+value).run(context->{
                assertThat(context).hasNotFailed();assertThat(context.getBean(com.company.crm.service.CustomerStatisticsService.class)
                    .getStatistics(7L,com.company.crm.enums.StatisticsTimeRange.ALL).totalCustomers()).isZero();
                assertThatThrownBy(()->context.getBean(CustomerRiskProperties.class).snapshot()).isInstanceOf(CustomerRiskException.class)
                    .extracting(e->((CustomerRiskException)e).getCode()).isEqualTo(CustomerRiskException.Code.NOT_READY);
            });
    }
    @Test void snapshotsAreImmutableAndVersionsReflectRules() {
        var properties=new CustomerRiskProperties();var before=properties.snapshot();
        assertThat(before.version()).isEqualTo(properties.snapshot().version());
        properties.setAMediumDays("15");var after=properties.snapshot();
        assertThat(before.aMediumDays()).isEqualTo(14);assertThat(after.aMediumDays()).isEqualTo(15);
        assertThat(after.version()).isNotEqualTo(before.version());
        properties.setMaxCandidates("0");assertThatThrownBy(properties::snapshot).isInstanceOf(CustomerRiskException.class);
    }
}
