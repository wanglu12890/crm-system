package com.company.crm.service;

import com.company.crm.entity.SysUser;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.CustomerRiskException;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.security.datascope.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CustomerRiskAccessPolicyTest {
    final SysUserMapper users=mock(SysUserMapper.class);
    final DataScopeResolver scopes=mock(DataScopeResolver.class);
    final CustomerRiskAccessPolicy policy=new CustomerRiskAccessPolicy(users,scopes);
    SysUser user;
    @BeforeEach void setup() {
        user=new SysUser();user.setId(7L);user.setStatus(1);user.setDeleted(0);user.setDeptId(20L);
        when(users.selectById(7L)).thenReturn(user);
        when(users.selectPermissionCodesByUserId(7L)).thenReturn(List.of("ai:analysis","customer:list","follow:list"));
    }
    @ParameterizedTest @CsvSource({
        "SALES_MANAGER,DEPT,CURRENT_AUTHORIZED,true", "SALES_MANAGER,DEPT,SELF,true", "SALES_MANAGER,DEPT,DEPT,true",
        "SALES_STAFF,SELF,CURRENT_AUTHORIZED,true", "SALES_STAFF,SELF,SELF,true", "SALES_STAFF,SELF,DEPT,false",
        "SALES_STAFF,SELF,ALL,false", "SALES_MANAGER,DEPT,ALL,false", "SALES_MANAGER,ALL,SELF,false",
        "SALES_MANAGER,SELF,SELF,false", "SALES_STAFF,DEPT,SELF,false", "SALES_STAFF,ALL,SELF,false",
        "SYSTEM_ADMIN,ALL,CURRENT_AUTHORIZED,false", "SUPER_ADMIN,ALL,SELF,false", "UNKNOWN,SELF,SELF,false",
        "SALES_MANAGER,DEPT,UNSUPPORTED,false"})
    void matrix(String role,DataScopeType granted,CustomerQueryScopeIntent requested,boolean allowed) {
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of(role));
        when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(granted,7L,20L));
        if (allowed) assertThat(policy.resolve(7L,requested).getScopeCode()).isEqualTo(requested==CustomerQueryScopeIntent.SELF ? "SELF" : granted.name());
        else assertDenied(requested);
    }
    void assertDenied(CustomerQueryScopeIntent scope) {assertThatThrownBy(()->policy.resolve(7L,scope)).isInstanceOf(CustomerRiskException.class);}
    @ParameterizedTest @ValueSource(strings={"ai:analysis","customer:list","follow:list"})
    void eachMissingPermissionDenied(String missing) {
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_STAFF"));
        when(users.selectPermissionCodesByUserId(7L)).thenReturn(List.of("ai:analysis","customer:list","follow:list").stream().filter(p->!p.equals(missing)).toList());
        assertDenied(CustomerQueryScopeIntent.SELF);verifyNoInteractions(scopes);
    }
    @Test void multiRoleInactiveDeletedAndMissingIdentityDenied() {
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_STAFF","SALES_MANAGER"));assertDenied(CustomerQueryScopeIntent.SELF);
        user.setStatus(0);assertDenied(CustomerQueryScopeIntent.SELF);
        user.setStatus(1);user.setDeleted(1);assertDenied(CustomerQueryScopeIntent.SELF);
        when(users.selectById(7L)).thenReturn(null);assertDenied(CustomerQueryScopeIntent.SELF);
        assertThatThrownBy(()->policy.resolve(null,CustomerQueryScopeIntent.SELF)).isInstanceOf(CustomerRiskException.class);
    }
    @Test void missingOrMismatchedDepartmentDeniedEvenForManagerSelf() {
        when(users.selectRoleCodesByUserId(7L)).thenReturn(List.of("SALES_MANAGER"));
        when(scopes.resolve(7L,"customer:list")).thenReturn(new DataScopeContext(DataScopeType.DEPT,7L,21L));
        assertDenied(CustomerQueryScopeIntent.SELF);user.setDeptId(null);assertDenied(CustomerQueryScopeIntent.DEPT);
    }
}
