package com.company.crm.service;

import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.AiUnsupportedAnalysisException;
import com.company.crm.exception.DataScopeConfigurationException;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.security.datascope.DataScopeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAnalysisScopePolicyTest {
    @Mock private DataScopeResolver resolver;
    @Mock private SysUserMapper users;
    private CustomerAnalysisScopePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new CustomerAnalysisScopePolicy(resolver, users);
    }

    @ParameterizedTest
    @CsvSource({
            "SALES_MANAGER,DEPT,SELF,true", "SALES_MANAGER,DEPT,DEPT,true",
            "SALES_MANAGER,DEPT,ALL,false", "SALES_STAFF,SELF,SELF,true",
            "SALES_STAFF,SELF,DEPT,false", "SALES_STAFF,SELF,ALL,false",
            "SYSTEM_ADMIN,ALL,SELF,false", "SYSTEM_ADMIN,ALL,DEPT,false", "SYSTEM_ADMIN,ALL,ALL,true",
            "SUPER_ADMIN,ALL,SELF,false", "SUPER_ADMIN,ALL,DEPT,false", "SUPER_ADMIN,ALL,ALL,true",
            "UNKNOWN,ALL,ALL,false", "SALES_MANAGER,ALL,SELF,false", "SALES_MANAGER,SELF,SELF,false",
            "SALES_STAFF,DEPT,SELF,false", "SYSTEM_ADMIN,SELF,SELF,false", "SUPER_ADMIN,DEPT,DEPT,false"
    })
    void validatesRoleAndScopeMatrix(String role, DataScopeType authorized, CustomerQueryScopeIntent requested,
                                    boolean allowed) {
        DataScopeContext context = new DataScopeContext(authorized, 7L, authorized == DataScopeType.DEPT ? 20L : null);
        when(resolver.resolve(7L, "customer:list")).thenReturn(context);
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of(role));
        if (!allowed) {
            assertThatThrownBy(() -> policy.resolve(7L, requested)).isInstanceOf(AiUnsupportedAnalysisException.class);
            return;
        }
        DataScopeContext effective = policy.resolve(7L, requested);
        assertThat(effective.getScopeCode()).isEqualTo(requested.name());
        assertThat(effective.getUserId()).isEqualTo(7L);
        if (requested == CustomerQueryScopeIntent.SELF) {
            assertThat(effective.getDeptId()).isNull();
        } else {
            assertThat(effective).isSameAs(context);
        }
    }

    @ParameterizedTest
    @EnumSource(DataScopeType.class)
    void defaultScopePreservesAuthorizationWithoutRoleRestrictions(DataScopeType type) {
        DataScopeContext context = new DataScopeContext(type, 7L, type == DataScopeType.DEPT ? 20L : null);
        when(resolver.resolve(7L, "customer:list")).thenReturn(context);
        assertThat(policy.resolve(7L, CustomerQueryScopeIntent.CURRENT_AUTHORIZED)).isSameAs(context);
        verifyNoInteractions(users);
    }

    @Test
    void rejectsMissingAmbiguousAndMalformedRoles() {
        when(resolver.resolve(7L, "customer:list")).thenReturn(new DataScopeContext(DataScopeType.ALL, 7L, null));
        for (List<String> roles : Arrays.asList(null, List.<String>of(), List.of("SYSTEM_ADMIN", "SALES_MANAGER"),
                Arrays.asList((String) null))) {
            when(users.selectRoleCodesByUserId(7L)).thenReturn(roles);
            assertThatThrownBy(() -> policy.resolve(7L, CustomerQueryScopeIntent.ALL))
                    .isInstanceOf(AiUnsupportedAnalysisException.class);
        }
    }

    @Test
    void rejectsInvalidInputWithoutReadingAuthorization() {
        assertThatThrownBy(() -> policy.resolve(7L, null)).isInstanceOf(AiUnsupportedAnalysisException.class);
        assertThatThrownBy(() -> policy.resolve(7L, CustomerQueryScopeIntent.UNSUPPORTED))
                .isInstanceOf(AiUnsupportedAnalysisException.class);
        assertThatThrownBy(() -> policy.resolve(null, CustomerQueryScopeIntent.SELF))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(resolver, users);
    }

    @Test
    void authorizationFailureCannotBeBypassedByRequestingSelf() {
        var failure = new DataScopeConfigurationException("DEPT requires department");
        when(resolver.resolve(7L, "customer:list")).thenThrow(failure);
        assertThatThrownBy(() -> policy.resolve(7L, CustomerQueryScopeIntent.SELF)).isSameAs(failure);
        verifyNoInteractions(users);
    }
}
