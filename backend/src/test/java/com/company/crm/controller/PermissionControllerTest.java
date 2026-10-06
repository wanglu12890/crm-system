package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.security.RestAccessDeniedHandler;
import com.company.crm.service.PermissionService;
import com.company.crm.vo.permission.PermissionTreeVO;
import com.company.crm.vo.permission.PermissionListVO;
import com.company.crm.vo.permission.PermissionOverviewVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PermissionService permissionService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/permissions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnNestedPermissionTreeForAuthenticatedRequest() throws Exception {
        PermissionTreeVO child = node(
                2L, 1L, "user:list", "查看用户", "BUTTON", null, 1
        );
        PermissionTreeVO root = node(
                1L, 0L, "system:user", "用户管理", "MENU", "/system/user", 1
        );
        root.getChildren().add(child);
        when(permissionService.getPermissionTree()).thenReturn(List.of(root));

        mockMvc.perform(get("/permissions").with(user("admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority("permission:list"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].parentId").value("0"))
                .andExpect(jsonPath("$[0].permissionCode").value("system:user"))
                .andExpect(jsonPath("$[0].permissionName").value("用户管理"))
                .andExpect(jsonPath("$[0].permissionType").value("MENU"))
                .andExpect(jsonPath("$[0].routePath").value("/system/user"))
                .andExpect(jsonPath("$[0].sortOrder").value(1))
                .andExpect(jsonPath("$[0].children").isArray())
                .andExpect(jsonPath("$[0].children[0].id").value("2"))
                .andExpect(jsonPath("$[0].children[0].parentId").value("1"))
                .andExpect(jsonPath("$[0].children[0].permissionCode").value("user:list"))
                .andExpect(jsonPath("$[0].children[0].permissionName").value("查看用户"))
                .andExpect(jsonPath("$[0].children[0].permissionType").value("BUTTON"))
                .andExpect(jsonPath("$[0].children[0].routePath").doesNotExist())
                .andExpect(jsonPath("$[0].children[0].sortOrder").value(1))
                .andExpect(jsonPath("$[0].children[0].children").isArray())
                .andExpect(jsonPath("$[0].children[0].children").isEmpty());
    }

    @Test
    void shouldRejectPermissionTreeWithoutAuthority() throws Exception {
        mockMvc.perform(get("/permissions").with(user("admin")))
                .andExpect(status().isForbidden());
        verify(permissionService, never()).getPermissionTree();
    }

    @Test
    void shouldRejectSystemAdminEvenWhenPermissionListAuthorityWasAssigned() throws Exception {
        mockMvc.perform(get("/permissions").with(user("system-admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                        new SimpleGrantedAuthority("permission:list"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(permissionService, never()).getPermissionTree();
    }

    @Test
    void shouldRejectSuperAdminWithoutPermissionListAuthority() throws Exception {
        mockMvc.perform(get("/permissions").with(user("admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isForbidden());
        verify(permissionService, never()).getPermissionTree();
    }

    @Test
    void shouldReturnPermissionListForSuperAdminWithAuthority() throws Exception {
        PermissionListVO item = new PermissionListVO();
        item.setId(3L);
        item.setParentId(2L);
        item.setPermissionName("查看用户");
        item.setPermissionCode("user:list");
        item.setModuleId(2L);
        item.setModuleName("用户管理");
        item.setPermissionType("BUTTON");
        item.setStatus(1);
        item.setSortOrder(10);
        when(permissionService.getPermissionList("user", 2L, 1)).thenReturn(List.of(item));

        mockMvc.perform(get("/permissions/list")
                        .param("keyword", "user")
                        .param("moduleId", "2")
                        .param("status", "1")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                                new SimpleGrantedAuthority("permission:list"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("3"))
                .andExpect(jsonPath("$[0].parentId").value("2"))
                .andExpect(jsonPath("$[0].moduleId").value("2"))
                .andExpect(jsonPath("$[0].moduleName").value("用户管理"))
                .andExpect(jsonPath("$[0].permissionCode").value("user:list"))
                .andExpect(jsonPath("$[0].status").value(1));
    }

    @Test
    void shouldRejectAnonymousPermissionList() throws Exception {
        mockMvc.perform(get("/permissions/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verify(permissionService, never()).getPermissionList(any(), any(), any());
    }

    @Test
    void shouldRejectSystemAdminPermissionListEvenWithAuthority() throws Exception {
        mockMvc.perform(get("/permissions/list").with(user("system-admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                        new SimpleGrantedAuthority("permission:list"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(permissionService, never()).getPermissionList(any(), any(), any());
    }

    @Test
    void shouldRejectSuperAdminPermissionListWithoutAuthority() throws Exception {
        mockMvc.perform(get("/permissions/list").with(user("admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isForbidden());
        verify(permissionService, never()).getPermissionList(any(), any(), any());
    }

    @Test
    void shouldRejectUnsupportedPermissionStatus() throws Exception {
        mockMvc.perform(get("/permissions/list")
                        .param("status", "2")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                                new SimpleGrantedAuthority("permission:list"))))
                .andExpect(status().isBadRequest());
        verify(permissionService, never()).getPermissionList(any(), any(), any());
    }

    @Test
    void shouldReturnPermissionOverviewForSuperAdminWithAuthority() throws Exception {
        PermissionOverviewVO overview = new PermissionOverviewVO(
                new PermissionOverviewVO.Summary(1, 1, 1),
                List.of(new PermissionOverviewVO.RoleItem(1L, "超级管理员", "SUPER_ADMIN", 1)),
                List.of(new PermissionOverviewVO.PermissionItem(
                        11L, 10L, "查看用户", "user:list", 10L, "用户管理", "BUTTON", 1, 1)),
                java.util.Map.of("1", List.of("11"))
        );
        when(permissionService.getPermissionOverview()).thenReturn(overview);

        mockMvc.perform(get("/permissions/overview").with(user("admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority("permission:list"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.permissionCount").value(1))
                .andExpect(jsonPath("$.summary.roleCount").value(1))
                .andExpect(jsonPath("$.summary.moduleCount").value(1))
                .andExpect(jsonPath("$.roles[0].id").value("1"))
                .andExpect(jsonPath("$.roles[0].roleCode").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.permissions[0].id").value("11"))
                .andExpect(jsonPath("$.permissions[0].moduleId").value("10"))
                .andExpect(jsonPath("$.rolePermissions.1[0]").value("11"));
    }

    @Test
    void shouldRejectAnonymousPermissionOverview() throws Exception {
        mockMvc.perform(get("/permissions/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verify(permissionService, never()).getPermissionOverview();
    }

    @Test
    void shouldRejectSystemAdminPermissionOverviewEvenWithAuthority() throws Exception {
        mockMvc.perform(get("/permissions/overview").with(user("system-admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"),
                        new SimpleGrantedAuthority("permission:list"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(permissionService, never()).getPermissionOverview();
    }

    @Test
    void shouldRejectSuperAdminPermissionOverviewWithoutAuthority() throws Exception {
        mockMvc.perform(get("/permissions/overview").with(user("admin").authorities(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                .andExpect(status().isForbidden());
        verify(permissionService, never()).getPermissionOverview();
    }

    private PermissionTreeVO node(
            Long id,
            Long parentId,
            String code,
            String name,
            String type,
            String routePath,
            Integer sortOrder
    ) {
        PermissionTreeVO node = new PermissionTreeVO();
        node.setId(id);
        node.setParentId(parentId);
        node.setPermissionCode(code);
        node.setPermissionName(name);
        node.setPermissionType(type);
        node.setRoutePath(routePath);
        node.setSortOrder(sortOrder);
        return node;
    }
}
