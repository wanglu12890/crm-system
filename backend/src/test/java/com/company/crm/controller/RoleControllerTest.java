package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void shouldReturnRoleListForAuthenticatedRequest() throws Exception {
        when(roleService.listRoles()).thenReturn(List.of(
                new RoleListVO(1L, "超级管理员", "SUPER_ADMIN", 1, 0L, "系统角色")
        ));

        mockMvc.perform(get("/roles").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].roleName").value("超级管理员"))
                .andExpect(jsonPath("$[0].roleCode").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$[0].status").value(1))
                .andExpect(jsonPath("$[0].permissionCount").value(0))
                .andExpect(jsonPath("$[0].remark").value("系统角色"));
    }
}
