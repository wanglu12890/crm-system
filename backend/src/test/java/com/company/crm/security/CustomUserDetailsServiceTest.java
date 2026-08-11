package com.company.crm.security;

import com.company.crm.entity.SysUser;
import com.company.crm.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoadActiveUser() {
        SysUser user = createUser(1, 0);
        when(sysUserMapper.selectByUsername("admin")).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("admin");

        assertThat(result).isInstanceOf(SecurityUser.class);
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("bcrypt-password");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    void shouldRejectMissingUser() {
        when(sysUserMapper.selectByUsername("missing")).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("用户不存在");
    }

    @Test
    void shouldRejectDisabledUser() {
        when(sysUserMapper.selectByUsername("admin")).thenReturn(createUser(0, 0));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("admin"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("用户状态异常，禁止登录");
    }

    @Test
    void shouldRejectLogicallyDeletedUser() {
        when(sysUserMapper.selectByUsername("admin")).thenReturn(createUser(1, 1));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("admin"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("用户已删除，禁止登录");
    }

    private SysUser createUser(Integer status, Integer deleted) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("bcrypt-password");
        user.setRealName("系统管理员");
        user.setStatus(status);
        user.setDeleted(deleted);
        return user;
    }
}
