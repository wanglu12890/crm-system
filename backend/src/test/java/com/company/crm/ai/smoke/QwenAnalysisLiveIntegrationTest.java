package com.company.crm.ai.smoke;

import com.company.crm.ai.config.AiChatClientConfig;
import com.company.crm.ai.tool.CustomerStatisticsToolFactory;
import com.company.crm.dto.statistics.CustomerStatisticsResult;
import com.company.crm.enums.CustomerAnalysisIntent;
import com.company.crm.enums.StatisticsTimeRange;
import com.company.crm.exception.AiUnsupportedAnalysisException;
import com.company.crm.service.AiAnalysisService;
import com.company.crm.service.CustomerStatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.ZonedDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Explicit opt-in only; model is live, statistics are synthetic and never access a database. */
@SpringBootTest(classes = QwenAnalysisLiveIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("ai")
@EnabledIfEnvironmentVariable(named = "RUN_QWEN_ANALYSIS_INTEGRATION", matches = "(?i:true)")
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
class QwenAnalysisLiveIntegrationTest {
    @Autowired AiAnalysisService service;
    @Autowired CustomerStatisticsService statistics;

    @Test
    void shouldSelectRealToolAndRejectUnsupportedQuestion() {
        var time = ZonedDateTime.parse("2026-10-14T12:00:00+08:00[Asia/Shanghai]");
        when(statistics.getStatistics(7L, StatisticsTimeRange.CURRENT_MONTH)).thenReturn(
                new CustomerStatisticsResult(5, 1, 1, 1, 1, 1, "SELF", StatisticsTimeRange.CURRENT_MONTH,
                        time.withDayOfMonth(1), time.withDayOfMonth(1).plusMonths(1), time, "Asia/Shanghai"));
        var answer = service.analyze(7L, "本月新增了多少客户？");
        assertThat(answer.intent()).isEqualTo(CustomerAnalysisIntent.CUSTOMER_TOTAL);
        assertThat(answer.metrics().totalCustomers()).isEqualTo(5);
        verify(statistics).getStatistics(7L, StatisticsTimeRange.CURRENT_MONTH);
        clearInvocations(statistics);
        assertThatThrownBy(() -> service.analyze(7L, "查询所有客户的姓名和电话号码"))
                .isInstanceOf(AiUnsupportedAnalysisException.class);
        verifyNoInteractions(statistics);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({AiChatClientConfig.class, AiAnalysisService.class, CustomerStatisticsToolFactory.class})
    static class TestApplication {
        @Bean CustomerStatisticsService customerStatisticsService() { return mock(CustomerStatisticsService.class); }
    }
}
