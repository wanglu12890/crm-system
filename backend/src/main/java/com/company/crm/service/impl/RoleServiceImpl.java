package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.crm.dto.role.CreateRoleDTO;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysPermission;
import com.company.crm.entity.SysRolePermission;
import com.company.crm.exception.DuplicateRoleCodeException;
import com.company.crm.exception.ForbiddenRoleCreationException;
import com.company.crm.exception.ForbiddenRolePermissionException;
import com.company.crm.exception.InvalidRolePermissionException;
import com.company.crm.exception.RoleNotFoundException;
import com.company.crm.mapper.SysPermissionMapper;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysRolePermissionMapper;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleListVO> listRoles() {
        return sysRoleMapper.selectRoleList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getRolePermissionIds(Long roleId) {
        return sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        Long operatorId = authentication.getPrincipal() instanceof SecurityUser securityUser
                ? securityUser.getUserId()
                : null;
        Long roleCount = sysRoleMapper.selectCount(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getId, roleId)
                        .eq(SysRole::getDeleted, 0)
        );
        if (roleCount == 0) {
            log.warn("Update role permissions rejected: role not found, operator={}, roleId={}", operator, roleId);
            throw new RoleNotFoundException(roleId);
        }

        String targetRoleCode = sysRoleMapper.selectRoleCodeById(roleId);
        if (SUPER_ADMIN.equals(targetRoleCode)) {
            log.warn(
                    "Update role permissions rejected by scope: operatorId={}, roleId={}, reason={}",
                    operatorId, roleId, "SUPER_ADMIN permissions are immutable through the API"
            );
            throw new ForbiddenRolePermissionException();
        }

        List<Long> distinctPermissionIds = new LinkedHashSet<>(permissionIds).stream().toList();
        validatePermissionIds(roleId, distinctPermissionIds, operator);

        // 对比现有权限和请求权限，若相同则无需更新
        Set<Long> requestedPermissionIds = new LinkedHashSet<>(distinctPermissionIds);
        Set<Long> existingPermissionIds = new LinkedHashSet<>(
                sysRolePermissionMapper.selectEnabledPermissionIdsByRoleId(roleId)
        );
        if (existingPermissionIds.equals(requestedPermissionIds)) {
            return;
        }

        sysRolePermissionMapper.delete(
                Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getRoleId, roleId)
        );
        for (Long permissionId : distinctPermissionIds) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            sysRolePermissionMapper.insert(relation);
        }

        log.info(
                "Update role permissions success, operator={}, roleId={}, permissionCount={}",
                operator, roleId, distinctPermissionIds.size()
        );
    }

    private void validatePermissionIds(Long roleId, List<Long> permissionIds, String operator) {
        if (permissionIds.isEmpty()) {
            return;
        }
        // 查找所有已存在的权限
        List<SysPermission> validPermissions = sysPermissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery()
                        .in(SysPermission::getId, permissionIds)
                        .eq(SysPermission::getStatus, 1)
        );
        // 检查权限是否有效
        Set<Long> validPermissionIds = validPermissions.stream()
                .map(SysPermission::getId)
                .collect(java.util.stream.Collectors.toSet());
        // 检查无效权限
        List<Long> invalidPermissionIds = permissionIds.stream()
                .filter(permissionId -> !validPermissionIds.contains(permissionId))
                .toList();
        // 若存在无效权限，则抛出异常
        if (!invalidPermissionIds.isEmpty()) {
            log.warn(
                    "Update role permissions rejected: invalid permissions, operator={}, roleId={}, invalidPermissionIds={}",
                    operator, roleId, invalidPermissionIds
            );
            throw new InvalidRolePermissionException(invalidPermissionIds);
        }
    }

    @Override
    public Long createRole(CreateRoleDTO dto) {
        String roleName = dto.getRoleName().trim();
        String roleCode = dto.getRoleCode().trim().toUpperCase();
        String remark = dto.getRemark() == null ? null : dto.getRemark().trim();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        Long operatorId = authentication.getPrincipal() instanceof SecurityUser securityUser
                ? securityUser.getUserId()
                : null;
        validateRoleCreationScope(authentication, operatorId, roleCode);

        Long existingCount = sysRoleMapper.selectCount(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, roleCode)
                        .eq(SysRole::getDeleted, 0)
        );
        if (existingCount > 0) {
            log.warn("Create role rejected: roleCode already exists, operator={}, roleCode={}", operator, roleCode);
            throw new DuplicateRoleCodeException(roleCode);
        }

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

    /** 方法级权限是第一道防线；Service 再校验真实操作者角色，避免绕过 Controller。 */
    private void validateRoleCreationScope(Authentication authentication, Long operatorId, String roleCode) {
        SecurityUser operator = authentication.getPrincipal() instanceof SecurityUser securityUser
                ? securityUser
                : null;
        boolean isSuperAdmin = operator != null && operator.getRoles().contains(SUPER_ADMIN);

        if (!isSuperAdmin) {
            rejectRoleCreation(operatorId, roleCode, "仅 SUPER_ADMIN 可以通过接口创建角色");
        }
        if (SUPER_ADMIN.equals(roleCode)) {
            rejectRoleCreation(operatorId, roleCode, "SUPER_ADMIN 只能由系统初始化逻辑创建");
        }
    }

    private void rejectRoleCreation(Long operatorId, String roleCode, String reason) {
        log.warn(
                "Create role rejected by scope: operatorId={}, roleCode={}, reason={}",
                operatorId, roleCode, reason
        );
        throw new ForbiddenRoleCreationException(roleCode);
    }
}
