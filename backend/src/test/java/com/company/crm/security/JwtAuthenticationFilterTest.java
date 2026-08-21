package com.company.crm.security;

import com.company.crm.config.SecurityConfig;
import com.company.crm.controller.AuthController;
import com.company.crm.exception.AuthExceptionHandler;
import com.company.crm.service.AuthService;
import com.company.crm.vo.auth.TokenVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        JwtAuthenticationFilterTest.ProtectedTestController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        AuthExceptionHandler.class,
        JwtAuthenticationFilterTest.ProtectedTestController.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef",
        "jwt.expire=3600"
})
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        securityUser = new SecurityUser(
                1L, "admin", "bcrypt-password", "系统管理员", 1, 0, List.of(),List.of(),List.of()
        );
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(securityUser);
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRestoreAuthenticationFromValidToken() throws Exception {
        String token = jwtService.generateAccessToken(securityUser);

        mockMvc.perform(get("/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void shouldTreatNonBearerAuthorizationAsUnauthenticated() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Basic credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() throws Exception {
        mockMvc.perform(get("/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnUnauthorizedForExpiredToken() throws Exception {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret(TEST_SECRET);
        expiredProperties.setExpire(-1);
        String expiredToken = new JwtService(expiredProperties).generateAccessToken(securityUser);

        mockMvc.perform(get("/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldAllowAnonymousLoginAndUseIssuedTokenForProtectedRequest() throws Exception {
        String token = jwtService.generateAccessToken(securityUser);
        when(authService.login(any())).thenReturn(new TokenVO(token, 3600));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String issuedToken = loginResponse.get("accessToken").asText();

        mockMvc.perform(get("/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + issuedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @RestController
    public static class ProtectedTestController {

        @GetMapping("/test/protected")
        public Map<String, String> protectedEndpoint(Authentication authentication) {
            SecurityUser principal = (SecurityUser) authentication.getPrincipal();
            return Map.of("username", principal.getUsername());
        }
    }
}
