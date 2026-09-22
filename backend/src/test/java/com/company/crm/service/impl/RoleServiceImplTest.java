package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.company.crm.dto.role.CreateRoleDTO;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysPermission;
import com.company.crm.entity.SysRolePermission;
import com.company.crm.exception.DuplicateRoleCodeException;
import com.company.crm.exception.ForbiddenRoleCreationException;
import com.company.crm.exception.ForbiddenRolePermissionException;
import com.company.crm.exception.InvalidRolePermissionException;
import com.company.crm.exception.RoleNotFoundException;
import com.company.crm.mapper.SysPermissionMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Mock
    private SysPermissionMapper sysPermissionMapper;

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
    void shouldReplaceRolePermissions() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysRoleMapper.selectRoleCodeById(10L)).thenReturn("SYSTEM_ADMIN");
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                permission(101L, 1), permission(104L, 1)
        ));
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L))
                .thenReturn(List.of(101L, 102L, 103L));

        roleService.updateRolePermissions(10L, List.of(101L, 104L));

        verify(sysRolePermissionMapper).delete(any(Wrapper.class));
        var relationCaptor = org.mockito.ArgumentCaptor.forClass(
                com.company.crm.entity.SysRolePermission.class
        );
        verify(sysRolePermissionMapper, times(2)).insert(relationCaptor.capture());
        assertThat(relationCaptor.getAllValues())
                .extracting(com.company.crm.entity.SysRolePermission::getPermissionId)
                .containsExactly(101L, 104L);
        assertThat(relationCaptor.getAllValues())
                .allSatisfy(relation -> assertThat(relation.getRoleId()).isEqualTo(10L));
    }

    @Test
    void shouldClearAllRolePermissions() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysRoleMapper.selectRoleCodeById(10L)).thenReturn("SALES_MANAGER");
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L))
                .thenReturn(List.of(101L, 102L));

        roleService.updateRolePermissions(10L, List.of());

        verify(sysRolePermissionMapper).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
        verify(sysPermissionMapper, never()).selectList(any());
    }

    @Test
    void shouldDeduplicatePermissionIdsBeforeInsert() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                permission(101L, 1), permission(102L, 1)
        ));
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L)).thenReturn(List.of());

        roleService.updateRolePermissions(10L, List.of(101L, 101L, 102L));

        verify(sysRolePermissionMapper, times(2)).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldRejectInvalidPermissionBeforeDeletingOldRelations() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(permission(101L, 1)));

        assertThatThrownBy(() -> roleService.updateRolePermissions(10L, List.of(101L, 999L)))
                .isInstanceOf(InvalidRolePermissionException.class);
        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldRejectDisabledPermissionBeforeDeletingOldRelations() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> roleService.updateRolePermissions(10L, List.of(103L)))
                .isInstanceOf(InvalidRolePermissionException.class);
        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void shouldRejectMissingRoleWithoutChangingRelations() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> roleService.updateRolePermissions(99L, List.of(101L)))
                .isInstanceOf(RoleNotFoundException.class);
        verify(sysPermissionMapper, never()).selectList(any());
        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldRejectUpdatingSuperAdminPermissionsWithoutWritingRelations() {
        authenticate(9L, "admin", List.of("SUPER_ADMIN"));
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysRoleMapper.selectRoleCodeById(1L)).thenReturn("SUPER_ADMIN");

        assertThatThrownBy(() -> roleService.updateRolePermissions(1L, List.of(101L)))
                .isInstanceOf(ForbiddenRolePermissionException.class)
                .hasMessage("不允许修改 SUPER_ADMIN 自身权限");
        verify(sysPermissionMapper, never()).selectList(any());
        verify(sysRolePermissionMapper, never()).selectEnabledPermissionIdsByRoleId(any());
        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldSkipWriteWhenPermissionSetIsExactlyTheSame() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                permission(101L, 1), permission(102L, 1), permission(103L, 1)
        ));
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L))
                .thenReturn(List.of(101L, 102L, 103L));

        roleService.updateRolePermissions(10L, List.of(101L, 102L, 103L));

        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldSkipWriteWhenOnlyPermissionOrderDiffers() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                permission(101L, 1), permission(102L, 1), permission(103L, 1)
        ));
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L))
                .thenReturn(List.of(101L, 102L, 103L));

        roleService.updateRolePermissions(10L, List.of(103L, 101L, 102L));

        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldSkipWriteWhenUserRestoresOriginalPermissionSet() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                permission(101L, 1), permission(102L, 1), permission(103L, 1)
        ));
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L))
                .thenReturn(List.of(101L, 102L, 103L));

        roleService.updateRolePermissions(10L, List.of(101L, 102L, 103L));

        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldSkipWriteWhenExistingAndRequestedPermissionsAreBothEmpty() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L)).thenReturn(List.of());

        roleService.updateRolePermissions(10L, List.of());

        verify(sysRolePermissionMapper, never()).delete(any(Wrapper.class));
        verify(sysRolePermissionMapper, never()).insert(any(SysRolePermission.class));
    }

    @Test
    void shouldDeclareTransactionForPermissionReplacement() throws NoSuchMethodException {
        Transactional transactional = RoleServiceImpl.class
                .getMethod("updateRolePermissions", Long.class, List.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }

    @Test
    void shouldPropagateInsertFailureSoTransactionCanRollBackDeletedRelations() {
        authenticate(9L, "admin");
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(sysPermissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(permission(101L, 1)));
        when(sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(10L)).thenReturn(List.of());
        when(sysRolePermissionMapper.insert(any(SysRolePermission.class)))
                .thenThrow(new RuntimeException("insert failed"));

        assertThatThrownBy(() -> roleService.updateRolePermissions(10L, List.of(101L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("insert failed");
        verify(sysRolePermissionMapper).delete(any(Wrapper.class));
    }

    @Test
    void shouldCreateRoleWithNormalizedFieldsAndCurrentOperator() {
        authenticate(9L, "admin", List.of("SUPER_ADMIN"));
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
        authenticate(9L, "admin", List.of("SUPER_ADMIN"));
        CreateRoleDTO dto = createRoleDto("系统管理员", "SYSTEM_ADMIN", 1, null);
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> roleService.createRole(dto))
                .isInstanceOf(DuplicateRoleCodeException.class)
                .hasMessage("角色编码 SYSTEM_ADMIN 已存在");
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    void shouldTranslateDatabaseUniqueConstraintConflict() {
        authenticate(9L, "admin", List.of("SUPER_ADMIN"));
        CreateRoleDTO dto = createRoleDto("系统管理员", "SYSTEM_ADMIN", 1, null);
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(sysRoleMapper.insert(any(SysRole.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> roleService.createRole(dto))
                .isInstanceOf(DuplicateRoleCodeException.class)
                .hasMessage("角色编码 SYSTEM_ADMIN 已存在");
    }

    @Test
    void shouldAllowSuperAdminToCreateBusinessRole() {
        authenticate(9L, "admin", List.of("SUPER_ADMIN"));
        stubSuccessfulRoleInsert();

        assertThat(roleService.createRole(createRoleDto("Sales manager", "SALES_MANAGER", 1, null)))
                .isEqualTo(100L);
        verify(sysRoleMapper).insert(any(SysRole.class));
    }

    @Test
    void shouldRejectSuperAdminRoleCreationEvenForSuperAdmin() {
        authenticate(9L, "admin", List.of("SUPER_ADMIN"));
        assertForbiddenWithoutInsert("SUPER_ADMIN");
    }

    @Test
    void shouldRejectSystemAdminCreatingSystemAdmin() {
        authenticate(10L, "system-admin", List.of("SYSTEM_ADMIN"));
        assertForbiddenWithoutInsert("SYSTEM_ADMIN");
    }

    @Test
    void shouldRejectSystemAdminCreatingSuperAdmin() {
        authenticate(10L, "system-admin", List.of("SYSTEM_ADMIN"));
        assertForbiddenWithoutInsert("SUPER_ADMIN");
    }

    @Test
    void shouldRejectSystemAdminCreatingCustomBusinessRole() {
        authenticate(10L, "system-admin", List.of("SYSTEM_ADMIN"));
        assertForbiddenWithoutInsert("REGIONAL_OWNER");
    }

    @Test
    void shouldRejectSalesManagerCreatingArbitraryRole() {
        authenticate(11L, "sales-manager", List.of("SALES_MANAGER"));
        assertForbiddenWithoutInsert("CUSTOM_BUSINESS_ROLE");
    }

    @Test
    void shouldRejectSalesStaffCreatingArbitraryRole() {
        authenticate(12L, "sales-staff", List.of("SALES_STAFF"));
        assertForbiddenWithoutInsert("ANOTHER_BUSINESS_ROLE");
    }

    @Test
    void shouldUseSuperAdminScopeForMultiRoleOperator() {
        authenticate(9L, "admin", List.of("SYSTEM_ADMIN", "SUPER_ADMIN"));
        stubSuccessfulRoleInsert();

        assertThat(roleService.createRole(createRoleDto("System admin", "SYSTEM_ADMIN", 1, null)))
                .isEqualTo(100L);
        verify(sysRoleMapper).insert(any(SysRole.class));
    }

    private void authenticate(Long userId, String username) {
        authenticate(userId, username, List.of());
    }

    private void authenticate(Long userId, String username, List<String> roles) {
        SecurityUser principal = new SecurityUser(
                userId, username, "password", "系统管理员", 1, 0,
                roles, List.of(), List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private void stubSuccessfulRoleInsert() {
        when(sysRoleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(sysRoleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            ReflectionTestUtils.setField(invocation.<SysRole>getArgument(0), "id", 100L);
            return 1;
        });
    }

    private void assertForbiddenWithoutInsert(String roleCode) {
        assertThatThrownBy(() -> roleService.createRole(createRoleDto("Protected role", roleCode, 1, null)))
                .isInstanceOf(ForbiddenRoleCreationException.class)
                .hasMessage("当前用户无权创建角色 " + roleCode);
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
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

    private SysPermission permission(Long id, Integer status) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setStatus(status);
        return permission;
    }
}
