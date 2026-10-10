package com.company.crm.security;

import com.company.crm.dto.auth.LoginDTO;
import com.company.crm.dto.user.CreateUserDTO;
import com.company.crm.dto.user.ResetUserPasswordDTO;
import com.company.crm.entity.SysUser;
import com.company.crm.vo.auth.TokenVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveObjectLoggingTest {

    private static final String TEST_PASSWORD = "fictional-test-password";
    private static final String TEST_PASSWORD_HASH = "fictional-test-password-hash";
    private static final String TEST_ACCESS_TOKEN = "fictional.test.jwt";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExcludeCreateUserPasswordFromToStringWithoutChangingAccessorsOrJson() throws Exception {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("test-user");
        dto.setPassword(TEST_PASSWORD);

        assertThat(dto.getPassword()).isEqualTo(TEST_PASSWORD);
        assertThat(dto.toString()).doesNotContain(TEST_PASSWORD).doesNotContain("password=");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));
        assertThat(json.get("password").asText()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void shouldExcludeResetPasswordFromToStringWithoutChangingAccessorsOrJson() throws Exception {
        ResetUserPasswordDTO dto = new ResetUserPasswordDTO();
        dto.setNewPassword(TEST_PASSWORD);

        assertThat(dto.getNewPassword()).isEqualTo(TEST_PASSWORD);
        assertThat(dto.toString()).doesNotContain(TEST_PASSWORD).doesNotContain("newPassword=");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));
        assertThat(json.get("newPassword").asText()).isEqualTo(TEST_PASSWORD);
    }

    @Test
    void shouldExcludePasswordHashFromSysUserToStringWithoutChangingAccessors() {
        SysUser user = new SysUser();
        user.setUsername("test-user");
        user.setPasswordHash(TEST_PASSWORD_HASH);

        assertThat(user.getPasswordHash()).isEqualTo(TEST_PASSWORD_HASH);
        assertThat(user.toString()).doesNotContain(TEST_PASSWORD_HASH).doesNotContain("passwordHash=");
    }

    @Test
    void shouldRedactLoginPasswordAndAccessTokenFromRecordToString() {
        LoginDTO login = new LoginDTO("test-user", TEST_PASSWORD);
        TokenVO token = new TokenVO(TEST_ACCESS_TOKEN, 7200);

        assertThat(login.password()).isEqualTo(TEST_PASSWORD);
        assertThat(login.toString()).doesNotContain(TEST_PASSWORD).contains("password=***");
        assertThat(token.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
        assertThat(token.toString()).doesNotContain(TEST_ACCESS_TOKEN).contains("accessToken=***");
    }
}
