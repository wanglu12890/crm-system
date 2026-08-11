package com.company.crm.service.impl;

import com.company.crm.dto.auth.LoginDTO;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.security.JwtProperties;
import com.company.crm.security.JwtService;
import com.company.crm.security.SecurityUser;
import com.company.crm.vo.auth.TokenVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldAuthenticateGenerateTokenAndUpdateLastLoginTime() {
        SecurityUser user = new SecurityUser(
                1L, "admin", "bcrypt-password", "系统管理员", 1, 0, List.of()
        );
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities()
        );
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProperties.getExpire()).thenReturn(7200L);
        when(sysUserMapper.updateLastLoginAt(any(), any())).thenReturn(1);

        TokenVO result = authService.login(new LoginDTO(" admin ", "admin123456"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.expiresIn()).isEqualTo(7200L);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getValue().getPrincipal()).isEqualTo("admin");
        assertThat(authenticationCaptor.getValue().getCredentials()).isEqualTo("admin123456");
        verify(jwtService).generateAccessToken(user);
        verify(sysUserMapper).updateLastLoginAt(any(), any());
    }
}
