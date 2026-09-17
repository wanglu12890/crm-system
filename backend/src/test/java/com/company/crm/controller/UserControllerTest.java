package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.exception.DuplicateUsernameException;
import com.company.crm.exception.UserExceptionHandler;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
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

        mockMvc.perform(post("/users").with(user("admin"))
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
        mockMvc.perform(post("/users").with(user("admin"))
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

        mockMvc.perform(post("/users").with(user("admin"))
                        .contentType("application/json")
                        .content("""
                                {"username":"admin","realName":"管理员","password":"123456",
                                 "status":1,"roleIds":["1"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }
}
