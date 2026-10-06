package com.company.crm.service.impl;

import com.company.crm.entity.SysPermission;
import com.company.crm.mapper.SysPermissionMapper;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysRolePermission;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysRolePermissionMapper;
import com.company.crm.vo.permission.PermissionOverviewVO;
import com.company.crm.vo.permission.PermissionTreeVO;
import com.company.crm.vo.permission.PermissionListVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void shouldReturnEmptyTreeWhenNoPermissionsExist() {
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of());

        List<PermissionTreeVO> tree = permissionService.getPermissionTree();

        assertThat(tree).isEmpty();
    }

    @Test
    void shouldRecognizeParentIdZeroAsSingleRootWithNonNullChildren() {
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(
                permission(1L, 0L, "system", "系统管理", "MENU", "/system", 1)
        ));

        List<PermissionTreeVO> tree = permissionService.getPermissionTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getId()).isEqualTo(1L);
        assertThat(tree.get(0).getParentId()).isZero();
        assertThat(tree.get(0).getChildren()).isNotNull().isEmpty();
    }

    @Test
    void shouldBuildMultiLevelParentChildTreeAndKeepChildrenNonNull() {
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(
                permission(1L, 0L, "system", "系统管理", "MENU", "/system", 1),
                permission(2L, 1L, "user", "用户管理", "MENU", "/system/user", 1),
                permission(3L, 2L, "user:list", "查看用户", "BUTTON", null, 1)
        ));

        List<PermissionTreeVO> tree = permissionService.getPermissionTree();

        PermissionTreeVO root = tree.get(0);
        PermissionTreeVO child = root.getChildren().get(0);
        PermissionTreeVO grandchild = child.getChildren().get(0);

        assertThat(root.getPermissionCode()).isEqualTo("system");
        assertThat(child.getPermissionCode()).isEqualTo("user");
        assertThat(grandchild.getPermissionCode()).isEqualTo("user:list");
        assertThat(root.getChildren()).isNotNull();
        assertThat(child.getChildren()).isNotNull();
        assertThat(grandchild.getChildren()).isNotNull().isEmpty();
    }

    @Test
    void shouldKeepSiblingOrderBySortOrderThenIdFromMapperQuery() {
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(
                permission(1L, 0L, "system", "系统管理", "MENU", "/system", 1),
                permission(3L, 1L, "role", "角色管理", "MENU", "/system/role", 1),
                permission(2L, 1L, "user", "用户管理", "MENU", "/system/user", 2),
                permission(4L, 1L, "permission", "权限管理", "MENU", "/system/permission", 2)
        ));

        List<PermissionTreeVO> tree = permissionService.getPermissionTree();

        assertThat(tree.get(0).getChildren())
                .extracting(PermissionTreeVO::getId)
                .containsExactly(3L, 2L, 4L);

    }

    @Test
    void shouldKeepMultipleRootOrder() {
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(
                permission(10L, 0L, "customer", "客户管理", "MENU", "/customer", 1),
                permission(20L, 0L, "system", "系统管理", "MENU", "/system", 2)
        ));

        List<PermissionTreeVO> tree = permissionService.getPermissionTree();

        assertThat(tree)
                .extracting(PermissionTreeVO::getId)
                .containsExactly(10L, 20L);
        assertThat(tree)
                .allSatisfy(node -> assertThat(node.getChildren()).isNotNull().isEmpty());
    }

    @Test
    void shouldIgnoreOrphanNodeAccordingToCurrentImplementation() {
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(
                permission(2L, 999L, "orphan", "孤儿权限", "BUTTON", null, 1)
        ));

        List<PermissionTreeVO> tree = permissionService.getPermissionTree();

        assertThat(tree).isEmpty();
    }

    @Test
    void permissionListShouldReturnEnabledAndDisabledPermissionsWithoutFilters() {
        SysPermission enabled = permission(1L, 0L, "system", "系统管理", "MENU", "/system", 1);
        SysPermission disabled = permission(2L, 1L, "user:list", "查看用户", "BUTTON", null, 1);
        disabled.setStatus(0);
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(enabled, disabled));

        List<PermissionListVO> result = permissionService.getPermissionList(null, null, null);

        assertThat(result).extracting(PermissionListVO::getStatus).containsExactly(1, 0);
    }

    @Test
    void permissionListShouldFilterKeywordByName() {
        when(sysPermissionMapper.selectList(any())).thenReturn(permissionListFixture());

        List<PermissionListVO> result = permissionService.getPermissionList("查看用户", null, null);

        assertThat(result).extracting(PermissionListVO::getPermissionCode).containsExactly("user:list");
    }

    @Test
    void permissionListShouldFilterTrimmedKeywordByCodeIgnoringCase() {
        when(sysPermissionMapper.selectList(any())).thenReturn(permissionListFixture());

        List<PermissionListVO> result = permissionService.getPermissionList("  USER:LIST  ", null, null);

        assertThat(result).extracting(PermissionListVO::getPermissionName).containsExactly("查看用户");
    }

    @Test
    void permissionListShouldIgnoreBlankKeyword() {
        when(sysPermissionMapper.selectList(any())).thenReturn(permissionListFixture());

        List<PermissionListVO> result = permissionService.getPermissionList("   ", null, null);

        assertThat(result).hasSize(5);
    }

    @Test
    void permissionListShouldFilterByStatus() {
        List<SysPermission> fixture = permissionListFixture();
        fixture.get(4).setStatus(0);
        when(sysPermissionMapper.selectList(any())).thenReturn(fixture);

        assertThat(permissionService.getPermissionList(null, null, 1))
                .extracting(PermissionListVO::getStatus).containsOnly(1);
        assertThat(permissionService.getPermissionList(null, null, 0))
                .extracting(PermissionListVO::getPermissionCode).containsExactly("role:list");
    }

    @Test
    void permissionListShouldResolveAndFilterBusinessModuleAcrossDescendants() {
        when(sysPermissionMapper.selectList(any())).thenReturn(permissionListFixture());

        List<PermissionListVO> result = permissionService.getPermissionList(null, 2L, null);

        assertThat(result).extracting(PermissionListVO::getPermissionCode)
                .containsExactly("system:user", "user:list");
        assertThat(result).allSatisfy(permission -> {
            assertThat(permission.getModuleId()).isEqualTo(2L);
            assertThat(permission.getModuleName()).isEqualTo("用户管理");
        });
    }

    @Test
    void permissionListShouldReturnEmptyForUnknownModule() {
        when(sysPermissionMapper.selectList(any())).thenReturn(permissionListFixture());

        assertThat(permissionService.getPermissionList(null, 999L, null)).isEmpty();
    }

    @Test
    void permissionListShouldKeepOrphanWithNullModule() {
        SysPermission orphan = permission(9L, 999L, "orphan:list", "孤儿权限", "BUTTON", null, 1);
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(orphan));

        List<PermissionListVO> result = permissionService.getPermissionList(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModuleId()).isNull();
        assertThat(result.get(0).getModuleName()).isNull();
    }

    @Test
    void overviewShouldReturnRealConfigurationIncludingDisabledDataAndIgnoreInvalidRelations() {
        SysRole superAdmin = role(1L, "SUPER_ADMIN", "超级管理员", 1, 0);
        SysRole disabledSales = role(2L, "SALES_STAFF", "销售人员", 0, 0);
        SysRole deletedRole = role(3L, "DELETED", "已删除角色", 1, 1);
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(superAdmin, disabledSales, deletedRole));

        SysPermission root = permission(10L, 0L, "system:user", "用户管理", "MENU", "/system/user", 1);
        SysPermission active = permission(11L, 10L, "user:list", "查看用户", "BUTTON", null, 1);
        SysPermission disabled = permission(12L, 10L, "user:create", "新建用户", "BUTTON", null, 2);
        disabled.setStatus(0);
        SysPermission orphan = permission(13L, 999L, "orphan", "孤儿权限", "BUTTON", null, 3);
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of(root, active, disabled, orphan));
        when(sysRolePermissionMapper.selectList(any())).thenReturn(List.of(
                relation(1L, 11L),
                relation(1L, 999L),
                relation(2L, 12L),
                relation(3L, 11L),
                relation(999L, 11L)
        ));

        PermissionOverviewVO overview = permissionService.getPermissionOverview();

        assertThat(overview.getSummary().getPermissionCount()).isEqualTo(overview.getPermissions().size()).isEqualTo(4);
        assertThat(overview.getSummary().getRoleCount()).isEqualTo(overview.getRoles().size()).isEqualTo(2);
        assertThat(overview.getSummary().getModuleCount()).isEqualTo(1);
        assertThat(overview.getRoles()).extracting(PermissionOverviewVO.RoleItem::getRoleCode)
                .containsExactly("SUPER_ADMIN", "SALES_STAFF");
        assertThat(overview.getRoles().get(1).getStatus()).isZero();
        assertThat(overview.getPermissions()).extracting(PermissionOverviewVO.PermissionItem::getStatus)
                .contains(1, 0);
        assertThat(overview.getPermissions().stream()
                .filter(item -> item.getId().equals(13L)).findFirst().orElseThrow().getModuleId()).isNull();
        // SUPER_ADMIN 只返回真实关联的 P11，不会自动补齐数据库中的其他权限。
        assertThat(overview.getRolePermissions().get("1")).containsExactly("11");
        // 停用角色与停用权限之间的真实关联仍然保留。
        assertThat(overview.getRolePermissions().get("2")).containsExactly("12");
        assertThat(overview.getRolePermissions()).doesNotContainKeys("3", "999");
        verify(sysRoleMapper).selectList(any());
        verify(sysPermissionMapper).selectList(any());
        verify(sysRolePermissionMapper).selectList(any());
    }

    @Test
    void overviewShouldProvideEmptyPermissionListForEveryRoleWhenNoPermissionsExist() {
        when(sysRoleMapper.selectList(any())).thenReturn(List.of(
                role(1L, "SUPER_ADMIN", "超级管理员", 1, 0),
                role(2L, "SYSTEM_ADMIN", "系统管理员", 1, 0)
        ));
        when(sysPermissionMapper.selectList(any())).thenReturn(List.of());
        when(sysRolePermissionMapper.selectList(any())).thenReturn(List.of(relation(1L, 999L)));

        PermissionOverviewVO overview = permissionService.getPermissionOverview();

        assertThat(overview.getSummary().getPermissionCount()).isZero();
        assertThat(overview.getSummary().getRoleCount()).isEqualTo(2);
        assertThat(overview.getSummary().getModuleCount()).isZero();
        assertThat(overview.getPermissions()).isEmpty();
        assertThat(overview.getRolePermissions()).containsOnlyKeys("1", "2");
        assertThat(overview.getRolePermissions().values()).allSatisfy(ids -> assertThat(ids).isEmpty());
    }

    private List<SysPermission> permissionListFixture() {
        return new java.util.ArrayList<>(List.of(
                permission(1L, 0L, "system", "系统管理", "MENU", "/system", 1),
                permission(2L, 1L, "system:user", "用户管理", "MENU", "/system/user", 1),
                permission(4L, 1L, "system:role", "角色管理", "MENU", "/system/role", 2),
                permission(3L, 2L, "user:list", "查看用户", "BUTTON", null, 1),
                permission(5L, 4L, "role:list", "查看角色", "BUTTON", null, 1)
        ));
    }

    private SysRole role(Long id, String code, String name, Integer status, Integer deleted) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setStatus(status);
        role.setDeleted(deleted);
        return role;
    }

    private SysRolePermission relation(Long roleId, Long permissionId) {
        SysRolePermission relation = new SysRolePermission();
        relation.setRoleId(roleId);
        relation.setPermissionId(permissionId);
        return relation;
    }

    private SysPermission permission(
            Long id,
            Long parentId,
            String code,
            String name,
            String type,
            String routePath,
            Integer sortOrder
    ) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setParentId(parentId);
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setPermissionType(type);
        permission.setRoutePath(routePath);
        permission.setSortOrder(sortOrder);
        permission.setStatus(1);
        return permission;
    }
}
