package com.company.crm.service.impl;

import com.company.crm.entity.SysPermission;
import com.company.crm.mapper.SysPermissionMapper;
import com.company.crm.vo.permission.PermissionTreeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private SysPermissionMapper sysPermissionMapper;

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
