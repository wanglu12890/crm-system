package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.service.PermissionService;
import com.company.crm.vo.permission.PermissionTreeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class
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

        mockMvc.perform(get("/permissions").with(user("admin")))
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
