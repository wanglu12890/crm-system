package com.company.crm.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void shouldGenerateAndParseValidAccessToken() {
        JwtService jwtService = createService(3600);
        SecurityUser user = createUser();

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.getUsername(token)).isEqualTo("admin");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void shouldRejectTamperedToken() {
        JwtService jwtService = createService(3600);
        String token = jwtService.generateAccessToken(createUser());

        assertThat(jwtService.isTokenValid(token + "tampered")).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService jwtService = createService(-1);
        String token = jwtService.generateAccessToken(createUser());

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void shouldRejectBlankToken() {
        JwtService jwtService = createService(3600);

        assertThat(jwtService.isTokenValid(" ")).isFalse();
    }

    private JwtService createService(long expire) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_SECRET);
        properties.setExpire(expire);
        return new JwtService(properties);
    }

    private SecurityUser createUser() {
        return new SecurityUser(
                1L,
                "admin",
                "bcrypt-password",
                "系统管理员",
                1,
                0,
                List.of()
        );
    }
}
