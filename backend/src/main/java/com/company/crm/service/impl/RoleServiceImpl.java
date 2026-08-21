package com.company.crm.service.impl;

import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.service.RoleService;
import com.company.crm.vo.role.RoleListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleListVO> listRoles() {
        return sysRoleMapper.selectRoleList();
    }
}
