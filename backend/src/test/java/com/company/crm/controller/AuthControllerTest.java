package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.exception.AuthExceptionHandler;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.AuthService;
import com.company.crm.vo.auth.TokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        AuthExceptionHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldAllowAnonymousLogin() throws Exception {
        when(authService.login(any())).thenReturn(new TokenVO("access-token", 7200));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(7200));
    }

    @Test
    void shouldReturnUnauthorizedForWrongPassword() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldReturnUnauthorizedForMissingUser() throws Exception {
        when(authService.login(any())).thenThrow(new UsernameNotFoundException("用户不存在"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing","password":"password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRequireAuthenticationForOtherEndpoints() throws Exception {
        mockMvc.perform(get("/protected-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRequireAuthenticationForCurrentUser() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnAuthenticatedCurrentUserWithoutInternalSecurityFields() throws Exception {
        SecurityUser securityUser = new SecurityUser(
                1L,
                "admin",
                "bcrypt-password",
                "超级管理员",
                1,
                0,
                List.of()
        );

        mockMvc.perform(get("/auth/me").with(user(securityUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.realName").value("超级管理员"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles").isEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.deleted").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist());
    }
}
