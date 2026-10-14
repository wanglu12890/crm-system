package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.exception.AiAnalysisExceptionHandler;
import com.company.crm.exception.AiAnalysisNotReadyException;
import com.company.crm.exception.CustomerStatisticsDataException;
import com.company.crm.exception.DataScopeConfigurationException;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAccessDeniedHandler;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.AiAnalysisService;
import com.company.crm.vo.AiAnalysisVO;
import com.company.crm.enums.CustomerAnalysisIntent;
import com.company.crm.enums.StatisticsTimeRange;
import com.company.crm.exception.AiModelUnavailableException;
import com.company.crm.exception.AiAnalysisProtocolException;
import com.company.crm.exception.AiUnsupportedAnalysisException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAnalysisController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, AiAnalysisExceptionHandler.class})
class AiAnalysisControllerTest {

    @Test
    void shouldRedactQuestionFromDiagnosticString() {
        var request = new com.company.crm.dto.ai.AiAnalysisRequest("客户敏感问题");
        org.assertj.core.api.Assertions.assertThat(request.toString()).doesNotContain(request.question());
    }

    @Autowired private MockMvc mockMvc;
    @MockBean private AiAnalysisService aiAnalysisService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtService jwtService;

    @Test
    void shouldRejectAnonymousRequest() throws Exception {
        perform("{\"question\":\"本月新增了多少客户？\"}", null)
                .andExpect(status().isUnauthorized());
        verify(aiAnalysisService, never()).analyze(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectUserWithoutAiAnalysisAuthority() throws Exception {
        perform("{\"question\":\"本月新增了多少客户？\"}",
                securityUser(7L, "customer:list"))
                .andExpect(status().isForbidden());
        verify(aiAnalysisService, never()).analyze(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectUserWithoutCustomerListAuthority() throws Exception {
        perform("{\"question\":\"本月新增了多少客户？\"}",
                securityUser(7L, "ai:analysis"))
                .andExpect(status().isForbidden());
        verify(aiAnalysisService, never()).analyze(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPassTrustedPrincipalIdAndReturnNotReady() throws Exception {
        doThrow(new AiAnalysisNotReadyException())
                .when(aiAnalysisService).analyze(7L, "本月新增了多少客户？");

        perform("{\"question\":\"本月新增了多少客户？\",\"userId\":999}",
                securityUser(7L, "ai:analysis", "customer:list"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_ANALYSIS_NOT_READY"))
                .andExpect(jsonPath("$.detail").value("AI分析功能尚未就绪"));

        verify(aiAnalysisService).analyze(7L, "本月新增了多少客户？");
    }

    @Test
    void shouldRejectMissingBlankAndOverlongQuestions() throws Exception {
        SecurityUser user = securityUser(7L, "ai:analysis", "customer:list");
        perform("{}", user).andExpect(status().isBadRequest());
        perform("{\"question\":\"   \"}", user).andExpect(status().isBadRequest());
        perform("{\"question\":\"" + "问".repeat(1001) + "\"}", user)
                .andExpect(status().isBadRequest());
        verify(aiAnalysisService, never()).analyze(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldMapDataScopeFailureToForbiddenWithoutSuccessResult() throws Exception {
        doThrow(new DataScopeConfigurationException("当前用户没有可用的客户数据范围"))
                .when(aiAnalysisService).analyze(7L, "查询客户");

        perform("{\"question\":\"查询客户\"}", securityUser(7L, "ai:analysis", "customer:list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_CONFIGURATION_ERROR"));
    }

    @Test
    void shouldHideStatisticsFailureDetails() throws Exception {
        doThrow(new CustomerStatisticsDataException("internal aggregate details"))
                .when(aiAnalysisService).analyze(7L, "查询客户");

        perform("{\"question\":\"查询客户\"}", securityUser(7L, "ai:analysis", "customer:list"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("AI_STATISTICS_FAILED"))
                .andExpect(jsonPath("$.detail").value("客户统计暂时不可用"));
    }

    private org.springframework.test.web.servlet.ResultActions perform(String content, SecurityUser principal)
            throws Exception {
        var request = post("/ai/analysis").contentType(MediaType.APPLICATION_JSON).content(content);
        if (principal != null) {
            request.with(user(principal));
        }
        return mockMvc.perform(request);
    }

    private SecurityUser securityUser(Long userId, String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new SecurityUser(userId, "tester", "password", "测试用户", 1, 0,
                List.of(), List.of(authorities), granted);
    }

    @Test
    void shouldReturnFormalResponseUsingTrustedPrincipal() throws Exception {
        var time = java.time.ZonedDateTime.parse("2026-10-14T12:00:00+08:00[Asia/Shanghai]");
        var result = new com.company.crm.dto.statistics.CustomerStatisticsResult(
                48, 10, 15, 12, 8, 3, "SELF", StatisticsTimeRange.CURRENT_MONTH,
                time.withDayOfMonth(1), time.withDayOfMonth(1).plusMonths(1), time, "Asia/Shanghai");
        org.mockito.Mockito.when(aiAnalysisService.analyze(7L, "本月新增了多少客户？"))
                .thenReturn(AiAnalysisVO.from(CustomerAnalysisIntent.CUSTOMER_TOTAL, result));
        perform("{\"question\":\"本月新增了多少客户？\",\"userId\":999,\"dataScope\":\"ALL\"}",
                securityUser(7L, "ai:analysis", "customer:list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("本月新增客户共 48 个。"))
                .andExpect(jsonPath("$.intent").value("CUSTOMER_TOTAL"))
                .andExpect(jsonPath("$.metrics.totalCustomers").value(48))
                .andExpect(jsonPath("$.analysisContext.dataScope").value("SELF"))
                .andExpect(jsonPath("$.analysisContext.timeRange").value("CURRENT_MONTH"))
                .andExpect(jsonPath("$.analysisContext.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.analysisContext.referenceTime").isString())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.toolCalls").doesNotExist());
        verify(aiAnalysisService).analyze(7L, "本月新增了多少客户？");
    }

    @Test
    void shouldDistinguishUnsupportedMissingToolAndUnavailableModel() throws Exception {
        var user = securityUser(7L, "ai:analysis", "customer:list");
        doThrow(new AiUnsupportedAnalysisException()).when(aiAnalysisService).analyze(7L, "问题");
        perform("{\"question\":\"问题\"}", user).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AI_ANALYSIS_UNSUPPORTED"));
        doThrow(new AiAnalysisProtocolException()).when(aiAnalysisService).analyze(7L, "问题");
        perform("{\"question\":\"问题\"}", user).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_MODEL_RESPONSE_INVALID"));
        doThrow(new AiModelUnavailableException()).when(aiAnalysisService).analyze(7L, "问题");
        perform("{\"question\":\"问题\"}", user).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_MODEL_UNAVAILABLE"));
    }
}
