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
    CustomerRiskResponseAssembler.class, CustomerRiskAnalysisServiceImpl.class, AiRiskAdviceControllerTest.Client.class})
class AiRiskAdviceControllerTest {
    @TestConfiguration static class Client {
        @Bean("qwenChatClient") ChatClient client(ChatModel model) { return ChatClient.create(model); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AiRiskChatProperties flag;
    @MockBean ChatModel model;
    @MockBean com.company.crm.ai.config.RiskAdviceChatClientFactory adviceFactory;
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
        var adviceClient=ChatClient.create(model);
        when(adviceFactory.create(any())).thenReturn(adviceClient);
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
    static final String ADVICE="{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}";
    void adviceStages(String output) throws Exception {
        doReturn(text(classification("SUGGESTIONS","SELF","PRIORITY",null)),calls("getCustomerRiskAnalysis","{}",1),text(output))
            .when(model).call(any(Prompt.class));
    }
    @Test void successPreservesEveryCoreFieldAndFingerprint() throws Exception {
        stages("LIST","SELF","PRIORITY",null);
        var original=ok("{\"question\":\"查看重点关注客户\"}");
        clearInvocations(model,mapper);
        adviceStages(ADVICE);
        var advised=ok("{\"question\":\"给出建议\"}");
        assertThat(advised.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(advised.at("/advice/status").asText()).isEqualTo("AVAILABLE");
        assertThat(advised.at("/advice/items/0/customerId").asText()).isEqualTo(Long.toString(ID));
        assertThat(advised.at("/advice/items/0/suggestion").asText()).contains("核实客户当前需求");
        for(String field:List.of("records","page","analysisContext","resultFingerprint","normalizedQuery","answer"))
            assertThat(advised.get(field)).isEqualTo(original.get(field));
        verify(model,times(3)).call(any(Prompt.class));
        verify(mapper).selectCandidates(any(),any(),any(),any(),anyInt());
        verify(mapper,times(2)).selectVisibleCustomers(any(),eq(List.of(ID)));
        var capture=org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(model,times(3)).call(capture.capture());
        String payload=capture.getAllValues().get(2).getInstructions().get(1).getText();
        assertThat(payload).doesNotContain(Long.toString(ID),"合成客户","给出建议","SALES_STAFF","departmentId","customerNo");
    }
    @ParameterizedTest @ValueSource(strings={"null","not-json","{\"items\":[]}","{\"items\":[],\"extra\":true}"})
    void badAdviceDegradesWithoutLosingTrustedRecords(String output) throws Exception {
        adviceStages(output);
        var body=ok("{\"question\":\"给出建议\"}");
        assertThat(body.at("/status").asText()).isEqualTo("PARTIAL");
        assertThat(body.at("/advice/status").asText()).isEqualTo("UNAVAILABLE");
        assertThat(body.at("/advice/items").isEmpty()).isTrue();
        assertThat(body.at("/records/0/riskLevel").asText()).isEqualTo("HIGH");
        verify(model,times(3)).call(any(Prompt.class));
        verify(mapper).selectCandidates(any(),any(),any(),any(),anyInt());
        verify(mapper,times(2)).selectVisibleCustomers(any(),any());
    }
    @Test void modelFailureStillPerformsFinalAuthorization() throws Exception {
        when(model.call(any(Prompt.class))).thenReturn(text(classification("SUGGESTIONS","SELF","PRIORITY",null)),
            calls("getCustomerRiskAnalysis","{}",1)).thenThrow(new IllegalStateException("private model output"));
        var result=ok("{\"question\":\"给出建议\"}");
        assertThat(result.get("status").asText()).isEqualTo("PARTIAL");
        assertThat(result.toString()).doesNotContain("private model output");
        verify(mapper,times(2)).selectVisibleCustomers(any(),any());
        verify(mapper).selectCandidates(any(),any(),any(),any(),anyInt());
    }
    @ParameterizedTest @ValueSource(strings={"PERMISSION","ROLE","DEPARTMENT","SCOPE"})
    void revokedBeforeAdviceNeverSendsFacts(String kind) throws Exception {
        adviceStages(ADVICE);
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenAnswer(i->{
            change(kind); return List.of(row(ID,20));
        });
        request("{\"question\":\"给出建议\"}").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.records").doesNotExist());
        verify(model,times(2)).call(any(Prompt.class)); verifyNoInteractions(adviceFactory);
    }
    void change(String kind) {
        switch(kind) {
            case "PERMISSION" -> when(users.selectPermissionCodesByUserId(7L)).thenReturn(List.of("customer:list"));
            case "ROLE" -> when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SUPER_ADMIN"));
            case "DEPARTMENT" -> actor.setDeptId(21L);
            case "SCOPE" -> when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(DataScopeType.DEPT,7L,20L));
        }
    }
    @ParameterizedTest @ValueSource(strings={"PERMISSION","ROLE","DEPARTMENT","SCOPE"})
    void revokedDuringAdviceRefusesWholeResponse(String kind) throws Exception {
        var classified=text(classification("SUGGESTIONS","SELF","PRIORITY",null));
        when(model.call(any(Prompt.class))).thenReturn(classified,calls("getCustomerRiskAnalysis","{}",1))
            .thenAnswer(i->{change(kind);return text(ADVICE);});
        request("{\"question\":\"给出建议\"}").andExpect(status().isForbidden())
            .andExpect(jsonPath("$.records").doesNotExist()).andExpect(jsonPath("$.advice").doesNotExist());
        verify(model,times(3)).call(any(Prompt.class));
        verify(mapper).selectCandidates(any(),any(),any(),any(),anyInt());
    }
    @ParameterizedTest @ValueSource(strings={"OWNER","POOL","DELETED","INAPPLICABLE","PARTIAL"})
    void objectsChangedDuringAdviceRefuseWholeResponse(String kind) throws Exception {
        adviceStages(ADVICE);
        var before=List.of(new CustomerRiskVisibility(ID,7L,20L,"ACTIVE"));
        var after=kind.equals("OWNER")?List.of(new CustomerRiskVisibility(ID,8L,20L,"ACTIVE")):List.<CustomerRiskVisibility>of();
        doReturn(before,after).when(mapper).selectVisibleCustomers(any(),any());
        request("{\"question\":\"给出建议\"}").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RISK_RESULT_STALE")).andExpect(jsonPath("$.records").doesNotExist());
        verify(model,times(3)).call(any(Prompt.class));
    }
    @Test void partialVisibilityBeforeModelAndBatchQueryFailureCannotBecomePartial() throws Exception {
        adviceStages(ADVICE);
        doReturn(List.of()).when(mapper).selectVisibleCustomers(any(),any());
        request("{\"question\":\"建议\"}").andExpect(status().isConflict());
        verify(model,times(2)).call(any(Prompt.class)); verifyNoInteractions(adviceFactory);
        clearInvocations(model);
        adviceStages(ADVICE);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("private SQL")).when(mapper).selectVisibleCustomers(any(),any());
        request("{\"question\":\"建议\"}").andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.records").doesNotExist());
        verify(model,times(2)).call(any(Prompt.class));
    }
    @Test void failedAdviceAndRevocationStillRefuseWholeResponse() throws Exception {
        when(model.call(any(Prompt.class))).thenReturn(text(classification("SUGGESTIONS","SELF","PRIORITY",null)),
            calls("getCustomerRiskAnalysis","{}",1)).thenAnswer(i->{change("PERMISSION");throw new IllegalStateException();});
        request("{\"question\":\"建议\"}").andExpect(status().isForbidden()).andExpect(jsonPath("$.records").doesNotExist());
    }
    @Test void staleFingerprintNeverSendsFacts() throws Exception {
        adviceStages(ADVICE);
        request("{\"question\":\"建议\",\"expectedResultFingerprint\":\"old\"}").andExpect(status().isConflict());
        verify(model,times(2)).call(any(Prompt.class)); verifyNoInteractions(adviceFactory);
        verify(mapper,never()).selectVisibleCustomers(any(),any());
    }
    @Test void emptyPageRechecksContextWithoutAdviceOrObjectQuery() throws Exception {
        adviceStages(ADVICE);
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of());
        var body=ok("{\"question\":\"建议\"}");
        assertThat(body.at("/advice/status").asText()).isEqualTo("NOT_APPLICABLE");
        verify(model,times(2)).call(any(Prompt.class)); verifyNoInteractions(adviceFactory);
        verify(mapper,never()).selectVisibleCustomers(any(),any());
        verify(users,times(3)).selectPermissionCodesByUserId(7L);
    }
    @Test void structuredSuggestionsRemainNotReadyWithNoModelAndNoAdviceChecks() throws Exception {
        var result=mvc.perform(post("/customer-risks/analyses").with(user(principal(PERMS))).contentType("application/json")
            .content("{\"action\":\"SUGGESTIONS\"}")).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(json.readTree(result.getContentAsString()).at("/advice/unavailableCode").asText()).isEqualTo("RISK_ADVICE_NOT_READY");
        verifyNoInteractions(model,adviceFactory); verify(mapper,never()).selectVisibleCustomers(any(),any());
    }
    @Test void reasonAndListDoNotCreateAdviceTransport() throws Exception {
        ok("{\"question\":\"重点关注清单\"}");
        stages("REASON","SELF","PRIORITY",null);
        ok("{\"question\":\"客户原因\",\"customerId\":\""+ID+"\"}");
        verifyNoInteractions(adviceFactory); verify(mapper,never()).selectVisibleCustomers(any(),any());
    }
    @Test void losingOneOfTwoCustomersRejectsPageBeforeAdvice() throws Exception {
        adviceStages(ADVICE);
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of(row(ID,20),row(ID+1,20)));
        doReturn(List.of(new CustomerRiskVisibility(ID,7L,20L,"ACTIVE"))).when(mapper).selectVisibleCustomers(any(),any());
        request("{\"question\":\"给出建议\"}").andExpect(status().isConflict()).andExpect(jsonPath("$.records").doesNotExist());
        verify(model,times(2)).call(any(Prompt.class)); verifyNoInteractions(adviceFactory);
        verify(mapper).selectVisibleCustomers(any(),eq(List.of(ID,ID+1)));
        verify(mapper).selectCandidates(any(),any(),any(),any(),anyInt());
    }
}
