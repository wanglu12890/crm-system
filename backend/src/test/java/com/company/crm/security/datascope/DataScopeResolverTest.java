package com.company.crm.security.datascope;

import com.company.crm.entity.SysUser;
import com.company.crm.exception.DataScopeConfigurationException;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeResolverTest {

    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysUserMapper sysUserMapper;
    @InjectMocks private DataScopeResolver resolver;

    @Test
    void shouldPreferAllAcrossMultipleGrantingRoles() {
        when(sysRoleMapper.selectGrantedDataScopes(1L, "customer:list"))
                .thenReturn(List.of("SELF", "DEPT", "ALL"));
        DataScopeContext result = resolver.resolve(1L, "customer:list");
        assertThat(result.getScope()).isEqualTo(DataScopeType.ALL);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getDeptId()).isNull();
        verify(sysUserMapper, never()).selectById(1L);
    }

    @Test
    void shouldResolveSalesManager01Department() {
        assertDepartmentScope(2107447193143648258L, 2206070000000000001L);
    }

    @Test
    void shouldResolveSalesManager02Department() {
        assertDepartmentScope(2107682836507561985L, 2206070000000000002L);
    }

    @Test
    void shouldResolveSelfToCurrentUser() {
        when(sysRoleMapper.selectGrantedDataScopes(2107447512812527618L, "customer:list"))
                .thenReturn(List.of("SELF"));
        DataScopeContext result = resolver.resolve(2107447512812527618L, "customer:list");
        assertThat(result.getScope()).isEqualTo(DataScopeType.SELF);
        assertThat(result.getUserId()).isEqualTo(2107447512812527618L);
        assertThat(result.getDeptId()).isNull();
    }

    @Test
    void shouldRejectDepartmentScopeWhenDepartmentIsMissing() {
        long userId = 99L;
        when(sysRoleMapper.selectGrantedDataScopes(userId, "customer:list"))
                .thenReturn(List.of("DEPT"));
        when(sysUserMapper.selectById(userId)).thenReturn(user(userId, null));
        assertThatThrownBy(() -> resolver.resolve(userId, "customer:list"))
                .isInstanceOf(DataScopeConfigurationException.class)
                .hasMessageContaining("DEPT");
    }

    @Test
    void shouldRejectMissingOrUnsupportedGrantedScope() {
        when(sysRoleMapper.selectGrantedDataScopes(99L, "customer:list"))
                .thenReturn(List.of("CUSTOM"));
        assertThatThrownBy(() -> resolver.resolve(99L, "customer:list"))
                .isInstanceOf(DataScopeConfigurationException.class);
    }

    private void assertDepartmentScope(long userId, long deptId) {
        when(sysRoleMapper.selectGrantedDataScopes(userId, "customer:list"))
                .thenReturn(List.of("SELF", "DEPT"));
        when(sysUserMapper.selectById(userId)).thenReturn(user(userId, deptId));
        DataScopeContext result = resolver.resolve(userId, "customer:list");
        assertThat(result.getScope()).isEqualTo(DataScopeType.DEPT);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getDeptId()).isEqualTo(deptId);
    }

    private SysUser user(long userId, Long deptId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setDeptId(deptId);
        return user;
    }
}
