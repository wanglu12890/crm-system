package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.crm.dto.role.CreateRoleDTO;
import com.company.crm.entity.SysRole;
import com.company.crm.exception.DuplicateRoleCodeException;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.RoleService;
import com.company.crm.vo.role.RoleListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleListVO> listRoles() {
        return sysRoleMapper.selectRoleList();
    }

    @Override
    public Long createRole(CreateRoleDTO dto) {
        String roleName = dto.getRoleName().trim();
        String roleCode = dto.getRoleCode().trim().toUpperCase();
        String remark = dto.getRemark() == null ? null : dto.getRemark().trim();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();

        Long existingCount = sysRoleMapper.selectCount(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, roleCode)
                        .eq(SysRole::getDeleted, 0)
        );
        if (existingCount > 0) {
            log.warn("Create role rejected: roleCode already exists, operator={}, roleCode={}", operator, roleCode);
            throw new DuplicateRoleCodeException(roleCode);
        }

        Long operatorId = authentication.getPrincipal() instanceof SecurityUser securityUser
                ? securityUser.getUserId()
                : null;
        LocalDateTime now = LocalDateTime.now();
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setStatus(dto.getStatus());
        role.setRemark(remark);
        role.setDataScope("SELF");
        role.setCreatedBy(operatorId);
        role.setUpdatedBy(operatorId);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        role.setDeleted(0);
        role.setVersion(0);

        try {
            sysRoleMapper.insert(role);
        } catch (DuplicateKeyException exception) {
            // Service 预检查提升可读性；数据库唯一约束负责兜住并发创建竞态。
            log.warn("Create role rejected by unique constraint, operator={}, roleCode={}", operator, roleCode);
            throw new DuplicateRoleCodeException(roleCode);
        }
        log.info(
                "Create role success, operator={}, roleId={}, roleCode={}, roleName={}",
                operator, role.getId(), role.getRoleCode(), role.getRoleName()
        );
        return role.getId();
    }
}
