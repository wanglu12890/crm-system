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
import com.company.crm.vo.ai.AiAnalysisVO;
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
                org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
    }

    @Test
    void shouldRejectUserWithoutAiAnalysisAuthority() throws Exception {
        perform("{\"question\":\"本月新增了多少客户？\"}",
                securityUser(7L, "customer:list"))
                .andExpect(status().isForbidden());
        verify(aiAnalysisService, never()).analyze(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
    }

    @Test
    void shouldRejectUserWithoutCustomerListAuthority() throws Exception {
        perform("{\"question\":\"本月新增了多少客户？\"}",
                securityUser(7L, "ai:analysis"))
                .andExpect(status().isForbidden());
        verify(aiAnalysisService, never()).analyze(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
    }

    @Test
    void shouldPassTrustedPrincipalIdAndReturnNotReady() throws Exception {
        doThrow(new AiAnalysisNotReadyException())
                .when(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));

        perform("{\"question\":\"本月新增了多少客户？\",\"userId\":999}",
                securityUser(7L, "ai:analysis", "customer:list"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_ANALYSIS_NOT_READY"))
                .andExpect(jsonPath("$.detail").value("AI分析功能尚未就绪"));

        verify(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
    }

    @Test
    void shouldRejectMissingBlankAndOverlongQuestions() throws Exception {
        SecurityUser user = securityUser(7L, "ai:analysis", "customer:list");
        perform("{}", user).andExpect(status().isBadRequest());
        perform("{\"question\":\"   \"}", user).andExpect(status().isBadRequest());
        perform("{\"question\":\"" + "问".repeat(1001) + "\"}", user)
                .andExpect(status().isBadRequest());
        verify(aiAnalysisService, never()).analyze(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
    }

    @Test
    void shouldMapDataScopeFailureToForbiddenWithoutSuccessResult() throws Exception {
        doThrow(new DataScopeConfigurationException("当前用户没有可用的客户数据范围"))
                .when(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));

        perform("{\"question\":\"查询客户\"}", securityUser(7L, "ai:analysis", "customer:list"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_CONFIGURATION_ERROR"));
    }

    @Test
    void shouldHideStatisticsFailureDetails() throws Exception {
        doThrow(new CustomerStatisticsDataException("internal aggregate details"))
                .when(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));

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
        org.mockito.Mockito.when(aiAnalysisService.analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class)))
                .thenReturn(AiAnalysisVO.from(CustomerAnalysisIntent.CUSTOMER_TOTAL, result));
        perform("{\"question\":\"本月新增了多少客户？\",\"userId\":999,\"dataScope\":\"ALL\"}",
                securityUser(7L, "ai:analysis", "customer:list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("本人范围内，本月新增客户共 48 个。"))
                .andExpect(jsonPath("$.intent").value("CUSTOMER_TOTAL"))
                .andExpect(jsonPath("$.metrics.totalCustomers").value(48))
                .andExpect(jsonPath("$.analysisContext.dataScope").value("SELF"))
                .andExpect(jsonPath("$.analysisContext.timeRange").value("CURRENT_MONTH"))
                .andExpect(jsonPath("$.analysisContext.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.analysisContext.referenceTime").isString())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.toolCalls").doesNotExist());
        verify(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
    }

    @Test
    void shouldDistinguishUnsupportedMissingToolAndUnavailableModel() throws Exception {
        var user = securityUser(7L, "ai:analysis", "customer:list");
        doThrow(new AiUnsupportedAnalysisException()).when(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
        perform("{\"question\":\"问题\"}", user).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AI_ANALYSIS_UNSUPPORTED"));
        doThrow(new AiAnalysisProtocolException()).when(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
        perform("{\"question\":\"问题\"}", user).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_MODEL_RESPONSE_INVALID"));
        doThrow(new AiModelUnavailableException()).when(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
        perform("{\"question\":\"问题\"}", user).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_MODEL_UNAVAILABLE"));
    }

    @Test
    void riskExceptionsKeepTheirStatusAndCodeAtSharedChatEntry() throws Exception {
        var actor = securityUser(7L, "ai:analysis", "customer:list");
        for (var code : com.company.crm.exception.CustomerRiskException.Code.values()) {
            int expected = switch (code) {
                case INVALID_QUERY -> 400;
                case AUTHENTICATION_REQUIRED -> 401;
                case ACCESS_DENIED, ACCESS_CONTEXT_CHANGED -> 403;
                case SCOPE_UNSUPPORTED, QUERY_UNSUPPORTED, CUSTOMER_SELECTION_REQUIRED -> 422;
                case NOT_FOUND -> 404;
                case RESULT_STALE -> 409;
                case NOT_READY, CAPACITY_EXCEEDED -> 503;
                case INVALID_DATA -> 500;
            };
            String expectedCode = switch (code) {
                case INVALID_QUERY -> "RISK_REQUEST_INVALID";
                case AUTHENTICATION_REQUIRED -> "RISK_AUTHENTICATION_REQUIRED";
                case ACCESS_DENIED -> "RISK_ACCESS_DENIED";
                case SCOPE_UNSUPPORTED -> "RISK_SCOPE_UNSUPPORTED";
                case ACCESS_CONTEXT_CHANGED -> "RISK_ACCESS_CONTEXT_CHANGED";
                case QUERY_UNSUPPORTED -> "RISK_QUERY_UNSUPPORTED";
                case CUSTOMER_SELECTION_REQUIRED -> "RISK_CUSTOMER_SELECTION_REQUIRED";
                case NOT_FOUND -> "RISK_CUSTOMER_NOT_FOUND";
                case RESULT_STALE -> "RISK_RESULT_STALE";
                case NOT_READY -> "RISK_RULES_NOT_READY";
                case CAPACITY_EXCEEDED -> "RISK_CAPACITY_EXCEEDED";
                case INVALID_DATA -> "RISK_QUERY_FAILED";
            };
            doThrow(new com.company.crm.exception.CustomerRiskException(code)).when(aiAnalysisService).analyze(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(com.company.crm.dto.ai.AiAnalysisRequest.class));
            perform("{\"question\":\"问题\"}", actor).andExpect(status().is(expected))
                    .andExpect(jsonPath("$.code").value(expectedCode))
                    .andExpect(jsonPath("$.records").doesNotExist());
        }
    }
}
