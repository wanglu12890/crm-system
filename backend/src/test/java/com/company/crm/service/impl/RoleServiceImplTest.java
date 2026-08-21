package com.company.crm.service.impl;

import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.vo.role.RoleListVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void shouldReturnRolesFromMapper() {
        List<RoleListVO> expected = List.of(
                new RoleListVO(1L, "超级管理员", "SUPER_ADMIN", 1, 0L, "系统角色")
        );
        when(sysRoleMapper.selectRoleList()).thenReturn(expected);

        assertThat(roleService.listRoles()).isEqualTo(expected);
        verify(sysRoleMapper).selectRoleList();
    }
}
