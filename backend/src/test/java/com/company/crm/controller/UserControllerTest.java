package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.exception.DuplicateUsernameException;
import com.company.crm.exception.UserExceptionHandler;
import com.company.crm.exception.ForbiddenRoleAssignmentException;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.security.RestAccessDeniedHandler;
import com.company.crm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        UserExceptionHandler.class})
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtService jwtService;

    @Test
    void shouldDeserializeSnowflakeRoleIdsAndCreateUser() throws Exception {
        long roleId = 2098342914642452481L;
        when(userService.createUser(argThat(dto -> dto.getRoleIds().equals(java.util.List.of(roleId)))))
                .thenReturn(2098342914642452499L);

        mockMvc.perform(post("/users").with(user("admin").authorities(
                        authority("user:create"), authority("user:assign_role")))
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "zhangsan2",
                                  "realName": "张三",
                                  "password": "123456",
                                  "phone": "13900000000",
                                  "status": 1,
                                  "roleIds": ["2098342914642452481"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string("2098342914642452499"));

        verify(userService).createUser(argThat(dto -> dto.getRoleIds().get(0).equals(roleId)));
    }

    @Test
    void shouldRejectAnonymousCreateRequest() throws Exception {
        mockMvc.perform(post("/users").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectEmptyRolesByValidation() throws Exception {
        mockMvc.perform(post("/users").with(user("admin").authorities(
                        authority("user:create"), authority("user:assign_role")))
                        .contentType("application/json")
                        .content("""
                                {"username":"newuser","realName":"用户","password":"123456",
                                 "status":1,"roleIds":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictForDuplicateUsername() throws Exception {
        when(userService.createUser(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DuplicateUsernameException("admin"));

        mockMvc.perform(post("/users").with(user("admin").authorities(
                        authority("user:create"), authority("user:assign_role")))
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","realName":"管理员","password":"123456",
                                 "status":1,"roleIds":["1"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void shouldRejectAnonymousUserList() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
        verify(userService, never()).listUsers();
    }

    @Test
    void shouldRejectUserListWithoutAuthority() throws Exception {
        mockMvc.perform(get("/users").with(user("admin")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("访问被拒绝"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("当前用户无权访问该资源"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(userService, never()).listUsers();
    }

    @Test
    void shouldAllowUserListWithAuthority() throws Exception {
        when(userService.listUsers()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/users").with(user("admin").authorities(authority("user:list"))))
                .andExpect(status().isOk());
        verify(userService).listUsers();
    }

    @Test
    void shouldRejectUserCreationWithoutAuthority() throws Exception {
        mockMvc.perform(post("/users").with(user("admin"))
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"realName\":\"用户\",\"password\":\"123456\",\"status\":1,\"roleIds\":[\"1\"]}"))
                .andExpect(status().isForbidden());
        verify(userService, never()).createUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectUserCreationWithCreateAuthorityOnly() throws Exception {
        mockMvc.perform(post("/users").with(user("admin").authorities(authority("user:create")))
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"realName\":\"用户\",\"password\":\"123456\",\"status\":1,\"roleIds\":[\"1\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(userService, never()).createUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnForbiddenWhenRoleAssignmentIsOutsideOperatorScope() throws Exception {
        when(userService.createUser(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ForbiddenRoleAssignmentException(java.util.List.of("SUPER_ADMIN")));

        mockMvc.perform(post("/users").with(user("admin").authorities(
                        authority("user:create"), authority("user:assign_role")))
                        .contentType("application/json")
                        .content("{\"username\":\"newuser\",\"realName\":\"用户\",\"password\":\"123456\",\"status\":1,\"roleIds\":[\"1\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("用户创建失败"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ROLE_ASSIGNMENT_FORBIDDEN"));
    }

    private SimpleGrantedAuthority authority(String value) {
        return new SimpleGrantedAuthority(value);
    }
}
