package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.company.crm.dto.user.CreateUserDTO;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysUser;
import com.company.crm.entity.SysUserRole;
import com.company.crm.exception.DuplicateUsernameException;
import com.company.crm.exception.InvalidUserRoleException;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.mapper.SysUserRoleMapper;
import com.company.crm.security.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private SysUserRoleMapper sysUserRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserServiceImpl userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateSingleRoleUserWithEncodedPassword() {
        authenticate();
        CreateUserDTO dto = dto(List.of(101L));
        when(sysRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(role(101L)));
        when(passwordEncoder.encode("123456")).thenReturn("bcrypt-hash");
        assignUserIdOnInsert(201L);

        assertThat(userService.createUser(dto)).isEqualTo(201L);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("123456");
        ArgumentCaptor<SysUserRole> relationCaptor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(sysUserRoleMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getUserId()).isEqualTo(201L);
        assertThat(relationCaptor.getValue().getRoleId()).isEqualTo(101L);
    }

    @Test
    void shouldCreateMultipleRoleRelations() {
        authenticate();
        when(sysRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(role(101L), role(102L)));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        assignUserIdOnInsert(201L);

        userService.createUser(dto(List.of(101L, 102L)));

        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(sysUserRoleMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(SysUserRole::getRoleId)
                .containsExactly(101L, 102L);
    }

    @Test
    void shouldDeduplicateRoleIds() {
        authenticate();
        when(sysRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(role(101L), role(102L)));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        assignUserIdOnInsert(201L);

        userService.createUser(dto(List.of(101L, 101L, 102L)));

        verify(sysUserRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    void shouldRejectExistingUsernameWithoutWriting() {
        authenticate();
        when(sysUserMapper.selectByUsername("newuser")).thenReturn(new SysUser());

        assertThatThrownBy(() -> userService.createUser(dto(List.of(101L))))
                .isInstanceOf(DuplicateUsernameException.class);

        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void shouldRejectRequestContainingInvalidRole() {
        authenticate();
        when(sysRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(role(101L)));

        assertThatThrownBy(() -> userService.createUser(dto(List.of(101L, 999L))))
                .isInstanceOf(InvalidUserRoleException.class);

        verify(sysUserMapper, never()).insert(any(SysUser.class));
        verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void shouldRejectDisabledRole() {
        authenticate();
        when(sysRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> userService.createUser(dto(List.of(101L))))
                .isInstanceOf(InvalidUserRoleException.class);
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void shouldPropagateRelationInsertFailureAndDeclareTransaction() throws Exception {
        authenticate();
        when(sysRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(role(101L)));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        assignUserIdOnInsert(201L);
        when(sysUserRoleMapper.insert(any(SysUserRole.class))).thenThrow(new DataAccessResourceFailureException("insert failed"));

        assertThatThrownBy(() -> userService.createUser(dto(List.of(101L))))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(UserServiceImpl.class.getMethod("createUser", CreateUserDTO.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    private void assignUserIdOnInsert(Long id) {
        doAnswer(invocation -> {
            invocation.<SysUser>getArgument(0).setId(id);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));
    }

    private void authenticate() {
        SecurityUser principal = new SecurityUser(9L, "admin", "hash", "管理员", 1, 0,
                List.of("SUPER_ADMIN"), List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private CreateUserDTO dto(List<Long> roleIds) {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername(" newuser ");
        dto.setRealName(" 新用户 ");
        dto.setPassword("123456");
        dto.setPhone("13900000000");
        dto.setStatus(1);
        dto.setRoleIds(roleIds);
        return dto;
    }

    private SysRole role(Long id) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(1);
        role.setDeleted(0);
        return role;
    }
}
