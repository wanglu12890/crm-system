package com.company.crm.controller;

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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

/** Real HTTP parsing, method security, application service, S1 service and policy; database reads are mocked. */
@WebMvcTest(AiCustomerRiskController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, CustomerRiskExceptionHandler.class, CustomerRiskAnalysisServiceImpl.class,
        CustomerRiskResponseAssembler.class, CustomerRiskServiceImpl.class, CustomerRiskAccessPolicy.class, CustomerRiskEvaluator.class, CustomerRiskProperties.class})
class AiCustomerRiskControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean CustomUserDetailsService userDetails;
    @MockBean PasswordEncoder passwordEncoder;
    @MockBean JwtService jwtService;
    @MockBean SysUserMapper users;
    @MockBean DataScopeResolver scopes;
    @MockBean CustomerRiskMapper mapper;
    @MockBean Clock businessClock;
    private static final String URL = "/customer-risks/analyses";
    private static final long ID = 2300000000000000001L;
    private static final List<String> PERMISSIONS = List.of("ai:analysis", "customer:list", "follow:list");

    @BeforeEach void setup() {
        when(businessClock.instant()).thenReturn(Instant.parse("2026-10-23T04:00:00Z"));
        when(businessClock.getZone()).thenReturn(ZoneId.of("Asia/Shanghai"));
        var actor = new SysUser(); actor.setId(7L); actor.setStatus(1); actor.setDeleted(0); actor.setDeptId(20L);
        when(users.selectById(7L)).thenReturn(actor);
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_STAFF"));
        when(users.selectPermissionCodesByUserId(7L)).thenReturn(PERMISSIONS);
        when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(DataScopeType.SELF,7L,null));
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of(row(ID,20)));
    }
    CustomerRiskAggregate row(long id, int days) {
        var row = new CustomerRiskAggregate(); row.setId(id); row.setCustomerName("示例客户");
        row.setCustomerNo("C-1"); row.setCustomerStatus("ACTIVE");
        row.setCreatedAt(LocalDateTime.of(2026,10,23,12,0).minusDays(days)); return row;
    }
    SecurityUser principal(List<String> permissions) {
        return new SecurityUser(7L,"sales","unused","销售",1,0,List.of("SALES_STAFF"),permissions,
                permissions.stream().map(SimpleGrantedAuthority::new).toList());
    }
    ResultActions request(String body) throws Exception {
        return mvc.perform(post(URL).with(user(principal(PERMISSIONS))).contentType("application/json").content(body));
    }
    JsonNode response(String body) throws Exception {
        return json.readTree(request(body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @Test void anonymousAndEachMissingAuthorityAreRejectedBeforeReads() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("{\"action\":\"LIST\"}"))
                .andExpect(status().isUnauthorized());
        for (String missing : PERMISSIONS) {
            mvc.perform(post(URL).with(user(principal(PERMISSIONS.stream().filter(p -> !p.equals(missing)).toList())))
                    .contentType("application/json").content("{\"action\":\"LIST\"}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(mapper);
    }
    @Test void defaultsAndStringIdsAndExplicitNulls() throws Exception {
        var body = response("{\"action\":\"LIST\"}");
        assertThat(body.at("/page/size").intValue()).isEqualTo(20);
        assertThat(body.at("/records/0/customerId").textValue()).isEqualTo(Long.toString(ID));
        assertThat(body.at("/records/0/customerLevel").isNull()).isTrue();
        assertThat(body.at("/records/0/lastValidFollowAt").isNull()).isTrue();
        assertThat(body.at("/analysisContext/analysisTime").textValue()).endsWith("+08:00");
        assertThat(body.at("/analysisContext/authorizedScope").textValue()).isEqualTo("SELF");
        assertThat(body.at("/advice/source").isNull()).isTrue();
        assertThat(body.toString()).doesNotContain("departmentId", "userId", "SQL");
    }
    @Test void managerDefaultAndSelfKeepAuthorizedScope() throws Exception {
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_MANAGER"));
        when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(DataScopeType.DEPT,7L,20L));
        for (String requested : List.of("CURRENT_AUTHORIZED","SELF")) {
            var context = response("{\"action\":\"LIST\",\"scopeIntent\":\""+requested+"\"}").get("analysisContext");
            assertThat(context.get("authorizedScope").textValue()).isEqualTo("DEPT");
            assertThat(context.get("requestedScope").textValue()).isEqualTo(requested);
            assertThat(context.get("effectiveScope").textValue()).isEqualTo(requested.equals("SELF")?"SELF":"DEPT");
        }
    }
    @Test void invalidRoleAndMissingDatabasePermissionDenied() throws Exception {
        for (var roles : List.of(List.of("SUPER_ADMIN"),List.of("SYSTEM_ADMIN"),List.of("SALES_MANAGER","SALES_STAFF"))) {
            when(users.selectRoleCodesByUserId(7L)).thenReturn(roles);
            request("{\"action\":\"LIST\"}").andExpect(status().isForbidden());
        }
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_STAFF"));
        for(String missing:PERMISSIONS) {
            when(users.selectPermissionCodesByUserId(7L)).thenReturn(PERMISSIONS.stream().filter(p->!p.equals(missing)).toList());
            request("{\"action\":\"LIST\"}").andExpect(status().isForbidden());
        }
        verifyNoInteractions(mapper);
    }
    @Test void scopeAndDepartmentErrorsAreDistinct() throws Exception {
        request("{\"action\":\"LIST\",\"scopeIntent\":\"DEPT\"}").andExpect(status().isUnprocessableEntity());
        request("{\"action\":\"LIST\",\"scopeIntent\":\"ALL\"}").andExpect(status().isUnprocessableEntity());
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_MANAGER"));
        request("{\"action\":\"LIST\"}").andExpect(status().isForbidden());
        verifyNoInteractions(mapper);
    }
    @Test void priorityAllStatesSizeAndOffPageTotal() throws Exception {
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of(row(ID,2),row(ID+1,20)));
        assertThat(response("{\"action\":\"LIST\"}").at("/page/total").intValue()).isEqualTo(1);
        var all = response("{\"action\":\"LIST\",\"queryMode\":\"ALL_STATES\",\"size\":50}");
        assertThat(all.at("/page/total").intValue()).isEqualTo(2);
        assertThat(all.at("/records/1/riskLevel").isNull()).isTrue();
        assertThat(all.at("/records/1/thresholdDays").isNull()).isTrue();
        var off = response("{\"action\":\"LIST\",\"page\":2147483647}");
        assertThat(off.at("/page/total").intValue()).isEqualTo(1);
        assertThat(off.get("records").isEmpty()).isTrue();
    }
    @Test void reasonKeepsSpecialStatesAndNotFoundIsUniform() throws Exception {
        when(mapper.selectCandidates(any(),any(),any(),eq(ID),anyInt())).thenReturn(List.of(row(ID,2)));
        var body = response("{\"action\":\"REASON\",\"customerId\":\""+ID+"\"}");
        assertThat(body.get("page").isNull()).isTrue();
        assertThat(body.get("normalizedQuery").size()).isEqualTo(2);
        assertThat(body.get("normalizedQuery").has("page")).isFalse();
        assertThat(body.at("/records/0/state").textValue()).isEqualTo("OBSERVING");
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of());
        for(String id:List.of("99","100")) request("{\"action\":\"REASON\",\"customerId\":\""+id+"\"}")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RISK_CUSTOMER_NOT_FOUND"));
    }
    @ParameterizedTest
    @ValueSource(strings={"{}","null","[]","{\"action\":\"NO\"}","{\"action\":1}",
        "{\"action\":\"LIST\",\"userId\":7}","{\"action\":\"LIST\",\"action\":\"LIST\"}",
        "{\"action\":\"LIST\",\"page\":1.0}","{\"action\":\"LIST\",\"page\":\"1\"}",
        "{\"action\":\"LIST\",\"page\":null}","{\"action\":\"LIST\",\"page\":0}",
        "{\"action\":\"LIST\",\"size\":51}","{\"action\":\"LIST\",\"customerLevels\":[]}",
        "{\"action\":\"LIST\",\"customerLevels\":[\"E\"]}","{\"action\":\"LIST\",\"customerLevels\":[null]}",
        "{\"action\":\"LIST\",\"queryMode\":\"ALL\"}","{\"action\":\"LIST\",\"scopeIntent\":\"UNKNOWN\"}",
        "{\"action\":\"REASON\",\"customerId\":1}","{\"action\":\"REASON\",\"customerId\":\"01\"}",
        "{\"action\":\"REASON\",\"customerId\":\"1e3\"}","{\"action\":\"REASON\",\"customerId\":\"9223372036854775808\"}",
        "{\"action\":\"REASON\"}","{\"action\":\"REASON\",\"customerId\":\"1\",\"page\":null}",
        "{\"action\":\"LIST\",\"customerId\":null}","{\"action\":\"LIST\",\"expectedResultFingerprint\":null}",
        "{\"action\":\"LIST\"} {}"})
    void strictRequestsFailBeforeDatabaseReads(String body) throws Exception {
        request(body).andExpect(status().isBadRequest()); verifyNoInteractions(mapper);
    }
    @Test void suggestionsFingerprintIsStableAcrossTimeAndDetectsChanges() throws Exception {
        var list = response("{\"action\":\"LIST\"}");
        String fingerprint = list.get("resultFingerprint").textValue();
        when(businessClock.instant()).thenReturn(Instant.parse("2026-10-23T04:01:00Z"));
        var suggestions = response("{\"action\":\"SUGGESTIONS\",\"expectedResultFingerprint\":\""+fingerprint+"\"}");
        assertThat(suggestions.get("status").textValue()).isEqualTo("PARTIAL");
        assertThat(suggestions.at("/advice/status").textValue()).isEqualTo("UNAVAILABLE");
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of(row(ID,21)));
        request("{\"action\":\"SUGGESTIONS\",\"expectedResultFingerprint\":\""+fingerprint+"\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.records").doesNotExist());
        when(mapper.selectCandidates(any(),any(),any(),any(),anyInt())).thenReturn(List.of());
        var empty=response("{\"action\":\"SUGGESTIONS\"}");
        assertThat(empty.get("status").textValue()).isEqualTo("SUCCESS");
        assertThat(empty.at("/advice/status").textValue()).isEqualTo("NOT_APPLICABLE");
    }
    @Test void serviceFailuresAreSafeHttpErrors() throws Exception {
        for(var code:List.of(CustomerRiskException.Code.NOT_READY,CustomerRiskException.Code.CAPACITY_EXCEEDED,
                CustomerRiskException.Code.INVALID_DATA)) {
            doThrow(new CustomerRiskException(code)).when(mapper).selectCandidates(any(),any(),any(),any(),anyInt());
            request("{\"action\":\"LIST\"}").andExpect(status().is(code==CustomerRiskException.Code.INVALID_DATA?500:503));
        }
        doThrow(new IllegalStateException("secret SQL")).when(mapper).selectCandidates(any(),any(),any(),any(),anyInt());
        request("{\"action\":\"LIST\"}").andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret SQL"))));
    }

    @Test void fingerprintCoversQueryMembershipAndTrustedDepartment() throws Exception {
        String original = response("{\"action\":\"LIST\"}").get("resultFingerprint").textValue();
        String resized = response("{\"action\":\"LIST\",\"size\":50}").get("resultFingerprint").textValue();
        assertThat(resized).isNotEqualTo(original);
        var actor = new SysUser(); actor.setId(7L); actor.setStatus(1); actor.setDeleted(0); actor.setDeptId(21L);
        when(users.selectById(7L)).thenReturn(actor);
        assertThat(response("{\"action\":\"LIST\"}").get("resultFingerprint").textValue()).isNotEqualTo(original);
        // Fingerprints never bypass a newly revoked permission.
        when(users.selectPermissionCodesByUserId(7L)).thenReturn(List.of("customer:list"));
        request("{\"action\":\"SUGGESTIONS\",\"expectedResultFingerprint\":\""+original+"\"}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.records").doesNotExist());
    }

    @Test void validLevelsNullAndReasonStatesKeepTheContract() throws Exception {
        request("{\"action\":\"LIST\",\"customerLevels\":null}").andExpect(status().isOk());
        request("{\"action\":\"LIST\",\"customerLevels\":[\"A\",\"UNRATED\"]}").andExpect(status().isOk());
        verify(mapper).selectCandidates(any(),any(),eq(Set.of("A","UNRATED")),isNull(),anyInt());
        var low = row(ID,100); low.setHistoryCount(1); low.setValidCount(1);
        low.setLastValidFollowAt(LocalDateTime.of(2026,10,22,12,0));
        when(mapper.selectCandidates(any(),any(),any(),eq(ID),anyInt())).thenReturn(List.of(low));
        String reason="{\"action\":\"REASON\",\"customerId\":\""+ID+"\"}";
        assertThat(response(reason).at("/records/0/riskLevel").textValue()).isEqualTo("LOW");
        var review=row(ID,100); review.setHistoryCount(1); review.setAnomalousCount(1);
        when(mapper.selectCandidates(any(),any(),any(),eq(ID),anyInt())).thenReturn(List.of(review));
        assertThat(response(reason).at("/records/0/state").textValue()).isEqualTo("DATA_REVIEW");
    }

    @ParameterizedTest
    @ValueSource(strings={"page","size","queryMode","customerLevels","expectedResultFingerprint"})
    void reasonRejectsEveryExplicitUnrelatedField(String field) throws Exception {
        request("{\"action\":\"REASON\",\"customerId\":\"1\",\""+field+"\":null}")
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mapper);
    }
}
