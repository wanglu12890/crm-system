package com.company.crm.controller;

import com.company.crm.ai.tool.CustomerRiskToolFactory;
import com.company.crm.config.*;
import com.company.crm.dto.risk.*;
import com.company.crm.entity.SysUser;
import com.company.crm.exception.*;
import com.company.crm.mapper.*;
import com.company.crm.security.*;
import com.company.crm.security.datascope.*;
import com.company.crm.service.*;
import com.company.crm.service.impl.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real unified entry, router, risk tool, core service, evaluator and assembler; mock model/SQL only. */
@WebMvcTest({AiAnalysisController.class, AiCustomerRiskController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class, AiAnalysisExceptionHandler.class, CustomerRiskExceptionHandler.class,
    AiAnalysisServiceImpl.class, AiIntentRouter.class, AiRiskAnalysisServiceImpl.class,
    RiskAdviceServiceImpl.class, CustomerRiskAdviceAccessVerifier.class, RiskAdviceProperties.class,
    CustomerRiskToolFactory.class, CustomerRiskServiceImpl.class, CustomerRiskAccessPolicy.class,
    CustomerRiskEvaluator.class, CustomerRiskProperties.class, AiRiskChatProperties.class,
    CustomerRiskResponseAssembler.class, CustomerRiskAnalysisServiceImpl.class, AiRiskAnalysisControllerTest.Client.class})
class AiRiskAnalysisControllerTest {
    @TestConfiguration static class Client {
        @Bean("qwenChatClient") ChatClient client(ChatModel model) { return ChatClient.create(model); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AiRiskChatProperties flag;
    @MockBean ChatModel model;
    @MockBean AiStatisticsAnalysisService statistics;
    @MockBean CustomUserDetailsService userDetails;
    @MockBean PasswordEncoder passwordEncoder;
    @MockBean JwtService jwtService;
    @MockBean SysUserMapper users;
    @MockBean DataScopeResolver scopes;
    @MockBean CustomerRiskMapper mapper;
    @MockBean Clock businessClock;
    static final List<String> PERMS = List.of("ai:analysis","customer:list","follow:list");
    static final long ID = 2300000000000000001L;
    SysUser actor;
    @BeforeEach void setup() throws Exception {
        flag.setEnabled(true);
        when(businessClock.instant()).thenReturn(Instant.parse("2026-10-24T04:00:00Z"));
        when(businessClock.getZone()).thenReturn(ZoneId.of("Asia/Shanghai"));
        actor = new SysUser(); actor.setId(7L); actor.setStatus(1); actor.setDeleted(0); actor.setDeptId(20L);
        when(users.selectById(7L)).thenReturn(actor);
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_STAFF"));
        when(users.selectPermissionCodesByUserId(7L)).thenReturn(PERMS);
        when(scopes.resolve(7L,"customer:list")).thenAnswer(i -> new DataScopeContext(DataScopeType.SELF,7L,null));
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of(row(ID,20)));
        when(mapper.selectVisibleCustomers(any(),any())).thenAnswer(i -> ((List<Long>)i.getArgument(1)).stream()
            .map(id -> new CustomerRiskVisibility(id,7L,20L,"ACTIVE")).toList());
        stages("LIST","CURRENT_AUTHORIZED","PRIORITY",null);
    }
    @AfterEach void after() { flag.setEnabled(false); }
    CustomerRiskAggregate row(long id,int days) {
        var r = new CustomerRiskAggregate(); r.setId(id); r.setCustomerName("合成客户"); r.setCustomerNo("SYN-1");
        r.setCustomerStatus("ACTIVE"); r.setCreatedAt(LocalDateTime.of(2026,10,24,12,0).minusDays(days)); return r;
    }
    SecurityUser principal(List<String> permissions) {
        return new SecurityUser(7L,"sales","unused","销售",1,0,List.of("SALES_STAFF"),permissions,
                permissions.stream().map(SimpleGrantedAuthority::new).toList());
    }
    ResultActions request(String body) throws Exception {
        return mvc.perform(post("/ai/analysis").with(user(principal(PERMS))).contentType("application/json").content(body));
    }
    JsonNode ok(String body) throws Exception {
        return json.readTree(request(body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    static ChatResponse text(String value) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(value))));
    }
    static ChatResponse calls(String name,String args,int count) {
        var calls = new ArrayList<AssistantMessage.ToolCall>();
        for(int i=0;i<count;i++) calls.add(new AssistantMessage.ToolCall("id"+i,"function",name,args));
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("不能当作事实").toolCalls(calls).build())));
    }
    String classification(String action,String scope,String mode,List<String> levels) throws Exception {
        return "{\"businessType\":\"RISK\",\"riskIntent\":\""+action+"\",\"queryScopeIntent\":\""+scope
            +"\",\"queryMode\":\""+mode+"\",\"customerLevels\":"+json.writeValueAsString(levels)+"}";
    }
    void stages(String action,String scope,String mode,List<String> levels) throws Exception {
        when(model.call(any(Prompt.class))).thenReturn(text(classification(action,scope,mode,levels)),
            calls("getCustomerRiskAnalysis","{}",1));
        clearInvocations(model);
    }
    void exactlyOneQuery() {
        verify(mapper).selectCandidates(any(),any(),any(),any(),eq(5001));
        verify(model,times(2)).call(any(Prompt.class));
        verifyNoInteractions(statistics);
    }
    @Test void listMatchesStructuredJsonAndFingerprintExactly() throws Exception {
        var chat = ok("{\"question\":\"查看重点关注客户\"}");
        var structured = json.readTree(mvc.perform(post("/customer-risks/analyses").with(user(principal(PERMS)))
            .contentType("application/json").content("{\"action\":\"LIST\"}")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        assertThat(chat).isEqualTo(structured);
        assertThat(chat.at("/records/0/customerId").textValue()).isEqualTo(Long.toString(ID));
        assertThat(chat.at("/advice/status").textValue()).isEqualTo("NOT_REQUESTED");
        verify(mapper,times(2)).selectCandidates(any(),any(),any(),any(),eq(5001)); // Two separate HTTP requests.
        verify(model,times(2)).call(any(Prompt.class)); // Structured endpoint has no model call.
    }
    @Test void toolSchemaIsEmptyAndPromptLoadsLiteralJsonWithoutTemplateParsing() throws Exception {
        ok("{\"question\":\"查看重点关注客户\"}");
        var capture = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(model,times(2)).call(capture.capture());
        var first = (ToolCallingChatOptions) capture.getAllValues().get(0).getOptions();
        var second = (ToolCallingChatOptions) capture.getAllValues().get(1).getOptions();
        assertThat(first.getToolCallbacks()).isEmpty();
        assertThat(second.getInternalToolExecutionEnabled()).isFalse();
        assertThat(second.getToolCallbacks()).hasSize(1);
        var definition = second.getToolCallbacks().get(0).getToolDefinition();
        assertThat(definition.name()).isEqualTo("getCustomerRiskAnalysis");
        assertThat(json.readTree(definition.inputSchema()).get("properties").isEmpty()).isTrue();
        assertThat(capture.getAllValues().get(1).getInstructions().get(0).getText()).contains("{}", "不得编造风险结果");
        exactlyOneQuery();
    }
    @Test void managerDefaultAndExplicitSelfAndStaffSelf() throws Exception {
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_MANAGER"));
        when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(DataScopeType.DEPT,7L,20L));
        for(String scope:List.of("CURRENT_AUTHORIZED","SELF")) {
            stages("LIST",scope,"PRIORITY",null);
            var result = ok("{\"question\":\"查看重点关注客户\"}");
            assertThat(result.at("/analysisContext/authorizedScope").textValue()).isEqualTo("DEPT");
            assertThat(result.at("/analysisContext/effectiveScope").textValue()).isEqualTo(scope.equals("SELF")?"SELF":"DEPT");
        }
    }
    @ParameterizedTest @ValueSource(strings={"SUPER_ADMIN","SYSTEM_ADMIN","MULTI"})
    void invalidRoleRejectedBeforeToolModelAndQuery(String role) throws Exception {
        when(users.selectRoleCodesByUserId(7L)).thenReturn(role.equals("MULTI")?List.of("SALES_STAFF","SALES_MANAGER"):List.of(role));
        request("{\"question\":\"查看重点关注客户\"}").andExpect(status().isForbidden());
        verify(model).call(any(Prompt.class)); verifyNoInteractions(mapper);
    }
    @ParameterizedTest @ValueSource(strings={"ai:analysis","customer:list","follow:list"})
    void databasePermissionRevokedBeforeToolRegistration(String missing) throws Exception {
        when(users.selectPermissionCodesByUserId(7L)).thenReturn(PERMS.stream().filter(p->!p.equals(missing)).toList());
        request("{\"question\":\"查看重点关注客户\"}").andExpect(status().isForbidden());
        verify(model).call(any(Prompt.class)); verifyNoInteractions(mapper);
    }
    @Test void missingControllerPermissionsPreventEvenClassification() throws Exception {
        for(String missing:List.of("ai:analysis","customer:list")) {
            mvc.perform(post("/ai/analysis").with(user(principal(PERMS.stream().filter(p->!p.equals(missing)).toList())))
                .contentType("application/json").content("{\"question\":\"风险\"}")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(model,mapper);
    }
    @ParameterizedTest @ValueSource(strings={"DEPT","ALL","UNSUPPORTED"})
    void staffScopeRejectedBeforeToolRegistration(String scope) throws Exception {
        stages("LIST",scope,"PRIORITY",null);
        request("{\"question\":\"风险\"}").andExpect(status().isUnprocessableEntity());
        verify(model).call(any(Prompt.class)); verifyNoInteractions(mapper);
    }
    @ParameterizedTest @ValueSource(strings={"PERMISSION","DEPARTMENT","ROLE","EFFECTIVE_SCOPE","IDENTITY","AUTHORIZED_SCOPE"})
    void contextChangedDuringModelWaitPreventsMapper(String change) throws Exception {
        String classified=classification("LIST","CURRENT_AUTHORIZED","PRIORITY",null);
        when(model.call(any(Prompt.class))).thenReturn(text(classified)).thenAnswer(i->{
            switch(change) {
                case "PERMISSION" -> when(users.selectPermissionCodesByUserId(7L)).thenReturn(List.of("customer:list"));
                case "DEPARTMENT" -> actor.setDeptId(21L);
                case "ROLE" -> when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_MANAGER"));
                case "EFFECTIVE_SCOPE" -> when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(DataScopeType.DEPT,7L,20L));
                case "IDENTITY" -> actor.setId(8L);
                case "AUTHORIZED_SCOPE" -> when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(DataScopeType.ALL,7L,null));
            }
            return calls("getCustomerRiskAnalysis","{}",1);
        });
        request("{\"question\":\"重点关注客户\"}").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RISK_ACCESS_CONTEXT_CHANGED"));
        verifyNoInteractions(mapper); verify(model,times(2)).call(any(Prompt.class));
    }
    @ParameterizedTest @ValueSource(strings={"{\"userId\":8}","{\"scope\":\"ALL\"}","{\"customerId\":\"8\"}","{\"page\":1}",
        "{\"x\":1,\"x\":2}","{} {}","[]","null","not-json",""})
    void invalidToolArgumentsCauseZeroQueries(String args) throws Exception {
        when(model.call(any(Prompt.class))).thenReturn(text(classification("LIST","SELF","PRIORITY",null)),
            calls("getCustomerRiskAnalysis",args,1));
        request("{\"question\":\"风险\"}").andExpect(status().isBadGateway());
        verifyNoInteractions(mapper);
    }
    @ParameterizedTest @CsvSource({"getCustomerStatistics,1","wrong,1","getCustomerRiskAnalysis,2","getCustomerRiskAnalysis,0"})
    void onlyOneExactRiskToolCallAllowed(String name,int count) throws Exception {
        when(model.call(any(Prompt.class))).thenReturn(text(classification("LIST","SELF","PRIORITY",null)),calls(name,"{}",count));
        request("{\"question\":\"风险\"}").andExpect(status().isBadGateway()); verifyNoInteractions(mapper);
    }
    @Test void modelTimeoutAndTextOnlyCannotFakeSuccess() throws Exception {
        when(model.call(any(Prompt.class))).thenReturn(text(classification("LIST","SELF","PRIORITY",null)))
            .thenThrow(new IllegalStateException("synthetic timeout"));
        request("{\"question\":\"风险\"}").andExpect(status().isServiceUnavailable()); verifyNoInteractions(mapper);
        when(model.call(any(Prompt.class))).thenReturn(text(classification("LIST","SELF","PRIORITY",null)),text("执行成功"));
        request("{\"question\":\"风险\"}").andExpect(status().isBadGateway()); verifyNoInteractions(mapper);
    }
    @Test void allStatesGlobalSortPageAndLevelFilterUseCore() throws Exception {
        var high = row(ID,20); high.setCustomerLevel("B");
        var medium = row(ID+1,8); medium.setCustomerLevel("A");
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of(row(ID+2,1),medium,high));
        stages("LIST","SELF","ALL_STATES",List.of("A","B"));
        var body = ok("{\"question\":\"查看A和B级全部关注状态\",\"page\":2,\"size\":1}");
        assertThat(body.at("/page/total").asInt()).isEqualTo(3);
        assertThat(body.at("/records/0/riskLevel").textValue()).isEqualTo("MEDIUM");
        verify(mapper).selectCandidates(any(),any(),eq(Set.of("A","B")),isNull(),anyInt());
        exactlyOneQuery();
    }
    @Test void priorityExcludesLowAndSpecialAndKeepsHighAndMedium() throws Exception {
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of(row(ID,1),row(ID+1,8),row(ID+2,20)));
        var body=ok("{\"question\":\"查看重点关注客户\"}");
        assertThat(body.at("/page/total").asInt()).isEqualTo(2);
        assertThat(body.at("/records/0/riskLevel").textValue()).isEqualTo("HIGH");
        assertThat(body.at("/records/1/riskLevel").textValue()).isEqualTo("MEDIUM");
        assertThat(body.at("/analysisContext/ruleVersion").textValue()).isNotBlank();
        exactlyOneQuery();
    }
    @Test void reasonExplicitIdAndJavaFactsIncludeSpecialState() throws Exception {
        stages("REASON","SELF","PRIORITY",null);
        when(mapper.selectCandidates(any(),any(),any(),eq(ID),anyInt())).thenReturn(List.of(row(ID,1)));
        var body=ok("{\"question\":\"为什么这个客户需要关注\",\"customerId\":\""+ID+"\"}");
        assertThat(body.at("/records/0/state").textValue()).isEqualTo("OBSERVING");
        assertThat(body.get("page").isNull()).isTrue(); exactlyOneQuery();
    }
    @ParameterizedTest @ValueSource(strings={"",",\"customerId\":7",",\"customerId\":\"01\"",",\"customerId\":\"9223372036854775808\"",",\"customerId\":null"})
    void missingOrInvalidReasonSelectionNeverQueries(String extra) throws Exception {
        stages("REASON","SELF","PRIORITY",null);
        request("{\"question\":\"查看客户原因\""+extra+"}").andExpect(status().isUnprocessableEntity());
        verify(model).call(any(Prompt.class)); verifyNoInteractions(mapper);
    }
    @Test void conflictingTextIdNeverQueries() throws Exception {
        stages("REASON","SELF","PRIORITY",null);
        request("{\"question\":\"查看客户ID: 99 的原因\",\"customerId\":\""+ID+"\"}")
            .andExpect(status().isUnprocessableEntity()); verifyNoInteractions(mapper);
    }
    @Test void reasonInvisibleMissingOrInapplicableShares404() throws Exception {
        stages("REASON","SELF","PRIORITY",null);
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of());
        request("{\"question\":\"查看原因\",\"customerId\":\""+ID+"\"}")
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RISK_CUSTOMER_NOT_FOUND")); exactlyOneQuery();
    }
    @ParameterizedTest @ValueSource(strings={",\"userId\":7",",\"role\":\"SALES_MANAGER\"",",\"deptId\":20",",\"effectiveScope\":\"ALL\"",
        ",\"size\":51",",\"size\":0",",\"page\":0",",\"page\":1.2",",\"size\":\"20\"",",\"size\":null",
        ",\"size\":20,\"size\":30",",\"customerId\":\"7\"",",\"queryMode\":\"ALL_STATES\""})
    void forbiddenControlsAndBadPlanNeverQueries(String extra) throws Exception {
        request("{\"question\":\"查看风险\""+extra+"}").andExpect(status().is4xxClientError());
        verify(model).call(any(Prompt.class)); verifyNoInteractions(mapper);
    }
    @Test void suggestionsReturnsOnlyTrustedFactsAndNeverCallsAdviceModel() throws Exception {
        stages("SUGGESTIONS","SELF","PRIORITY",null);
        var body=ok("{\"question\":\"给出客户跟进建议\"}");
        assertThat(body.get("status").textValue()).isEqualTo("PARTIAL");
        assertThat(body.at("/advice/status").textValue()).isEqualTo("UNAVAILABLE");
        assertThat(body.at("/advice/unavailableCode").textValue()).isEqualTo("RISK_ADVICE_NOT_READY");
        exactlyOneQuery();
    }
    @Test void emptySuggestionsAndStaleFingerprintKeepOriginalContract() throws Exception {
        stages("SUGGESTIONS","SELF","PRIORITY",null);
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of());
        var body=ok("{\"question\":\"给出跟进建议\",\"size\":50}");
        assertThat(body.at("/advice/status").textValue()).isEqualTo("NOT_APPLICABLE");
        assertThat(body.at("/page/total").asInt()).isZero();
        stages("SUGGESTIONS","SELF","PRIORITY",null);
        request("{\"question\":\"给出建议\",\"expectedResultFingerprint\":\"stale\"}").andExpect(status().isConflict());
        verify(mapper,times(2)).selectCandidates(any(),any(),any(),any(),anyInt());
    }
    @Test void disabledFlagPreservesB1ZeroQueryAndOneModelCall() throws Exception {
        flag.setEnabled(false);
        request("{\"question\":\"风险\"}").andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("AI_RISK_CHAT_DISABLED"));
        verify(model).call(any(Prompt.class)); verifyNoInteractions(mapper,users);
    }
}
