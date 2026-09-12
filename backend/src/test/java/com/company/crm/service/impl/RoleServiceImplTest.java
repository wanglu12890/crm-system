package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.company.crm.dto.role.CreateRoleDTO;
import com.company.crm.entity.SysRole;
import com.company.crm.exception.DuplicateRoleCodeException;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysRolePermissionMapper;
import com.company.crm.security.SecurityUser;
import com.company.crm.vo.role.RoleListVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnRolesFromMapper() {
        List<RoleListVO> expected = List.of(
                new RoleListVO(1L, "超级管理员", "SUPER_ADMIN", 1, 0L, "系统角色")
        );
        when(sysRoleMapper.selectRoleList()).thenReturn(expected);

        assertThat(roleService.listRoles()).isEqualTo(expected);
        verify(sysRoleMapper).selectRoleList();
    }

    @Test
    void shouldReturnRolePermissionIdsFromMapper() {
        List<Long> expected = List.of(2085985855238352897L, 2085985855238352898L);
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L)).thenReturn(expected);

        assertThat(roleService.getRolePermissionIds(10L)).isEqualTo(expected);
        verify(sysRolePermissionMapper).selectEnabledPermissionIdsByRoleId(10L);
    }

    @Test
    void shouldReturnEmptyRolePermissionIds() {
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L)).thenReturn(List.of());

        assertThat(roleService.getRolePermissionIds(10L)).isEmpty();
    }

    @Test
    void shouldCreateRoleWithNormalizedFieldsAndCurrentOperator() {
        authenticate(9L, "admin");
        CreateRoleDTO dto = createRoleDto(" 系统管理员 ", " system_admin ", 1, " 负责系统管理 ");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(sysRoleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            ReflectionTestUtils.setField(invocation.<SysRole>getArgument(0), "id", 100L);
            return 1;
        });

        Long roleId = roleService.createRole(dto);

        assertThat(roleId).isEqualTo(100L);
        var roleCaptor = org.mockito.ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).insert(roleCaptor.capture());
        SysRole role = roleCaptor.getValue();
        assertThat(role.getRoleName()).isEqualTo("系统管理员");
        assertThat(role.getRoleCode()).isEqualTo("SYSTEM_ADMIN");
        assertThat(role.getStatus()).isEqualTo(1);
        assertThat(role.getRemark()).isEqualTo("负责系统管理");
        assertThat(role.getDataScope()).isEqualTo("SELF");
        assertThat(role.getCreatedBy()).isEqualTo(9L);
        assertThat(role.getUpdatedBy()).isEqualTo(9L);
        assertThat(role.getCreatedAt()).isNotNull();
        assertThat(role.getUpdatedAt()).isNotNull();
        assertThat(role.getDeleted()).isZero();
        assertThat(role.getVersion()).isZero();
    }

    @Test
    void shouldRejectDuplicateRoleCodeWithoutInsert() {
        authenticate(9L, "admin");
        CreateRoleDTO dto = createRoleDto("系统管理员", "SYSTEM_ADMIN", 1, null);
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> roleService.createRole(dto))
                .isInstanceOf(DuplicateRoleCodeException.class)
                .hasMessage("角色编码 SYSTEM_ADMIN 已存在");
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    void shouldTranslateDatabaseUniqueConstraintConflict() {
        authenticate(9L, "admin");
        CreateRoleDTO dto = createRoleDto("系统管理员", "SYSTEM_ADMIN", 1, null);
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(sysRoleMapper.insert(any(SysRole.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> roleService.createRole(dto))
                .isInstanceOf(DuplicateRoleCodeException.class)
                .hasMessage("角色编码 SYSTEM_ADMIN 已存在");
    }

    private void authenticate(Long userId, String username) {
        SecurityUser principal = new SecurityUser(
                userId, username, "password", "系统管理员", 1, 0,
                List.of(), List.of(), List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private CreateRoleDTO createRoleDto(
            String roleName, String roleCode, Integer status, String remark
    ) {
        CreateRoleDTO dto = new CreateRoleDTO();
        dto.setRoleName(roleName);
        dto.setRoleCode(roleCode);
        dto.setStatus(status);
        dto.setRemark(remark);
        return dto;
    }
}
