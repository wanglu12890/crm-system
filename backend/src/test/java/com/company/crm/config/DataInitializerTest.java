package com.company.crm.config;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysUser;
import com.company.crm.entity.SysUserRole;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DataInitializerTest {

    private static final String VALID_TEST_PASSWORD = "test-bootstrap-password";
    private static final String TEST_PASSWORD_HASH = "test-password-hash";

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializationProperties properties;
    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        properties = new AdminInitializationProperties();
        initializer = new DataInitializer(
                sysRoleMapper,
                sysUserMapper,
                sysUserRoleMapper,
                passwordEncoder,
                properties
        );
    }

    @Test
    void shouldNotCreateAdministratorWhenInitializationIsDisabledByDefault() {
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role(1L));

        initializer.run();

        verifyNoInteractions(sysUserMapper, sysUserRoleMapper, passwordEncoder);
    }

    @Test
    void shouldNotCreateAdministratorWhenEnabledWithoutPassword() {
        properties.setEnabled(true);
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role(1L));
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        initializer.run();

        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verifyNoInteractions(sysUserRoleMapper, passwordEncoder);
    }

    @Test
    void shouldNotCreateAdministratorWhenExternalPasswordIsTooShort() {
        properties.setEnabled(true);
        properties.setPassword("too-short");
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role(1L));
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        initializer.run();

        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verifyNoInteractions(sysUserRoleMapper, passwordEncoder);
    }

    @Test
    void shouldCreateAdministratorAndRoleBindingWithEncodedExternalPassword() {
        enableWithValidPassword();
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role(10L));
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(passwordEncoder.encode(VALID_TEST_PASSWORD)).thenReturn(TEST_PASSWORD_HASH);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(20L);
            return 1;
        });

        initializer.run();

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("admin");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(TEST_PASSWORD_HASH);

        ArgumentCaptor<SysUserRole> relationCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(sysUserRoleMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getUserId()).isEqualTo(20L);
        assertThat(relationCaptor.getValue().getRoleId()).isEqualTo(10L);
    }

    @Test
    void shouldLeaveExistingAdministratorCredentialsAndBindingsUnchanged() {
        properties.setEnabled(true);
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role(1L));
        SysUser existingAdmin = new SysUser();
        existingAdmin.setId(2L);
        existingAdmin.setUsername("admin");
        existingAdmin.setPasswordHash("existing-hash");
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(existingAdmin);

        initializer.run();

        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verifyNoInteractions(sysUserRoleMapper, passwordEncoder);
        assertThat(existingAdmin.getPasswordHash()).isEqualTo("existing-hash");
    }

    @Test
    void shouldRemainIdempotentAcrossRepeatedRuns() {
        enableWithValidPassword();
        SysRole role = role(1L);
        SysUser createdAdmin = new SysUser();
        createdAdmin.setId(2L);
        createdAdmin.setUsername("admin");
        createdAdmin.setPasswordHash(TEST_PASSWORD_HASH);
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null, createdAdmin);
        when(passwordEncoder.encode(VALID_TEST_PASSWORD)).thenReturn(TEST_PASSWORD_HASH);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        });

        initializer.run();
        initializer.run();

        verify(sysUserMapper, times(1)).insert(any(SysUser.class));
        verify(sysUserRoleMapper, times(1)).insert(any(SysUserRole.class));
        verify(passwordEncoder, times(1)).encode(VALID_TEST_PASSWORD);
    }

    @Test
    void shouldInitializeBaseRoleEvenWhenAdministratorInitializationIsDisabled() {
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        initializer.run();

        ArgumentCaptor<SysRole> roleCaptor = ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).insert(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRoleCode()).isEqualTo("SUPER_ADMIN");
        verifyNoInteractions(sysUserMapper, sysUserRoleMapper, passwordEncoder);
    }

    @Test
    void shouldNotWriteInitialPasswordOrHashToLogs(CapturedOutput output) {
        enableWithValidPassword();
        when(sysRoleMapper.selectOne(any(Wrapper.class))).thenReturn(role(1L));
        when(sysUserMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(passwordEncoder.encode(VALID_TEST_PASSWORD)).thenReturn(TEST_PASSWORD_HASH);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        });

        initializer.run();

        assertThat(output.getAll())
                .doesNotContain(VALID_TEST_PASSWORD)
                .doesNotContain(TEST_PASSWORD_HASH);
    }

    private void enableWithValidPassword() {
        properties.setEnabled(true);
        properties.setPassword(VALID_TEST_PASSWORD);
    }

    private SysRole role(Long id) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode("SUPER_ADMIN");
        return role;
    }
}
