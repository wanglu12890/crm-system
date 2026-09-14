package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.exception.DuplicateRoleCodeException;
import com.company.crm.exception.InvalidRolePermissionException;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.service.RoleService;
import com.company.crm.vo.role.RoleListVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class
})
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectAnonymousRolePermissionRequest() throws Exception {
        mockMvc.perform(get("/roles/10/permissions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnRoleListForAuthenticatedRequest() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of(
                new RoleListVO(1L, "超级管理员", "SUPER_ADMIN", 1, 0L, "系统角色")
        ));

        mockMvc.perform(get("/roles").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].roleName").value("超级管理员"))
                .andExpect(jsonPath("$[0].roleCode").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$[0].status").value(1))
                .andExpect(jsonPath("$[0].permissionCount").value(0))
                .andExpect(jsonPath("$[0].remark").value("系统角色"));
    }

    @Test
    void shouldReturnRolePermissionIdsAsStrings() throws Exception {
        long roleId = 2085985855238352896L;
        when(roleService.getRolePermissionIds(roleId)).thenReturn(List.of(
                2085985855238352897L,
                2085985855238352898L
        ));

        mockMvc.perform(get("/roles/{roleId}/permissions", roleId).with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("2085985855238352897"))
                .andExpect(jsonPath("$[1]").value("2085985855238352898"));
    }

    @Test
    void shouldReturnEmptyPermissionIdsWhenRoleHasNoPermissions() throws Exception {
        when(roleService.getRolePermissionIds(10L)).thenReturn(List.of());

        mockMvc.perform(get("/roles/10/permissions").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldUpdateRolePermissions() throws Exception {
        mockMvc.perform(put("/roles/10/permissions")
                        .with(user("admin"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "permissionIds": [
                                    "2085985855238352897",
                                    "2085985855238352898"
                                  ]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(roleService).updateRolePermissions(
                10L,
                List.of(2085985855238352897L, 2085985855238352898L)
        );
    }

    @Test
    void shouldAllowClearingRolePermissions() throws Exception {
        mockMvc.perform(put("/roles/10/permissions")
                        .with(user("admin"))
                        .contentType("application/json")
                        .content("{\"permissionIds\": []}"))
                .andExpect(status().isNoContent());

        verify(roleService).updateRolePermissions(10L, List.of());
    }

    @Test
    void shouldRejectNullPermissionIds() throws Exception {
        mockMvc.perform(put("/roles/10/permissions")
                        .with(user("admin"))
                        .contentType("application/json")
                        .content("{\"permissionIds\": null}"))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).updateRolePermissions(any(), any());
    }

    @Test
    void shouldRejectAnonymousPermissionUpdate() throws Exception {
        mockMvc.perform(put("/roles/10/permissions")
                        .contentType("application/json")
                        .content("{\"permissionIds\": []}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBadRequestForInvalidPermission() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidRolePermissionException(List.of(999L)))
                .when(roleService).updateRolePermissions(10L, List.of(999L));

        mockMvc.perform(put("/roles/10/permissions")
                        .with(user("admin"))
                        .contentType("application/json")
                        .content("{\"permissionIds\": [\"999\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERMISSION"));
    }

    @Test
    void shouldCreateRoleForAuthenticatedRequest() throws Exception {
        when(roleService.createRole(any())).thenReturn(100L);

        mockMvc.perform(post("/roles")
                        .with(user("admin"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "roleName": "系统管理员",
                                  "roleCode": "SYSTEM_ADMIN",
                                  "status": 1,
                                  "remark": "负责系统用户及普通角色管理"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string("100"));
    }

    @Test
    void shouldRejectRoleWithoutRoleCode() throws Exception {
        mockMvc.perform(post("/roles")
                        .with(user("admin"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "roleName": "测试角色",
                                  "roleCode": "",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenRoleCodeAlreadyExists() throws Exception {
        when(roleService.createRole(any())).thenThrow(new DuplicateRoleCodeException("SYSTEM_ADMIN"));

        mockMvc.perform(post("/roles")
                        .with(user("admin"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "roleName": "系统管理员",
                                  "roleCode": "SYSTEM_ADMIN",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROLE_CODE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.detail").value("角色编码 SYSTEM_ADMIN 已存在"));
    }
}
