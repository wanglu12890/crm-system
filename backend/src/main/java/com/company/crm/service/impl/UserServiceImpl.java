package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.crm.dto.user.CreateUserDTO;
import com.company.crm.dto.user.UpdateUserDTO;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysUser;
import com.company.crm.entity.SysUserRole;
import com.company.crm.exception.DuplicateUsernameException;
import com.company.crm.exception.InvalidUserRoleException;
import com.company.crm.exception.ForbiddenRoleAssignmentException;
import com.company.crm.exception.ForbiddenUserUpdateException;
import com.company.crm.exception.UserNotFoundException;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysUserRoleMapper;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.UserService;
import com.company.crm.vo.user.UserListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    private static final String ROOT_ADMIN_USERNAME = "admin";
    private static final Set<String> SYSTEM_ROLE_CODES = Set.of(SUPER_ADMIN, SYSTEM_ADMIN);

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserListVO> listUsers() {
        return sysUserMapper.selectUserList();
    }

    @Override
    @Transactional
    public Long createUser(CreateUserDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication.getName();
        SecurityUser operatorUser = authentication.getPrincipal() instanceof SecurityUser securityUser
                ? securityUser
                : null;
        Long operatorId = operatorUser == null ? null : operatorUser.getUserId();
        String username = dto.getUsername().trim();

        // 数据库 username 唯一约束包含逻辑删除记录，因此这里检查全部记录，并由唯一索引兜底并发竞争。
        if (sysUserMapper.selectByUsername(username) != null) {
            log.warn("Create user rejected: username already exists, operator={}, username={}", operator, username);
            throw new DuplicateUsernameException(username);
        }

        // 角色 ID 去重并保持顺序
        List<Long> roleIds = new LinkedHashSet<>(dto.getRoleIds()).stream().toList();
        // 检查前端传入的角色 ID 是否存在且有效（状态为启用且未删除），validRoles 只是「查到的合法角色」，不包含「非法」信息。
        List<SysRole> validRoles = sysRoleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1)
                        .eq(SysRole::getDeleted, 0)
        );
        // 要判断「前端传的 ID 是否全合法」，必须拿传入的 ID 和合法 ID 对比。
        // 提取合法 ID 集合（存在、启用且未删除）
        Set<Long> validRoleIds = validRoles.stream().map(SysRole::getId).collect(Collectors.toSet());
        // 找出非法角色 ID（不存在、停用或已删除）
        List<Long> invalidRoleIds = roleIds.stream().filter(id -> !validRoleIds.contains(id)).toList();
        // 如果存在无效角色 ID，则抛出异常
        if (!invalidRoleIds.isEmpty()) {
            log.warn("Create user rejected: invalid roles, operator={}, username={}, invalidRoleIds={}",
                    operator, username, invalidRoleIds);
            throw new InvalidUserRoleException(invalidRoleIds);
        }

        validateAssignableRoles(operatorUser, validRoles, operator, username, "Create user");

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setRealName(dto.getRealName().trim());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setMobile(dto.getPhone() == null || dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        user.setStatus(dto.getStatus());
        user.setCreatedBy(operatorId);
        user.setUpdatedBy(operatorId);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        user.setVersion(0);

        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            log.warn("Create user rejected by unique constraint, operator={}, username={}", operator, username);
            throw new DuplicateUsernameException(username);
        }

        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(user.getId());
            relation.setRoleId(roleId);
            relation.setCreatedAt(now);
            sysUserRoleMapper.insert(relation);
        }

        log.info("Create user success, operator={}, userId={}, username={}, roleCount={}",
                operator, user.getId(), username, roleIds.size());
        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UpdateUserDTO dto) {
        SecurityUser operatorUser = currentSecurityUser();
        Long operatorId = operatorUser == null ? null : operatorUser.getUserId();
        List<String> operatorRoles = operatorUser == null ? List.of() : operatorUser.getRoles();

        SysUser targetUser = sysUserMapper.selectById(userId);
        if (targetUser == null) {
            throw new UserNotFoundException(userId);
        }

        List<String> targetRoleCodes = sysUserMapper.selectAllRoleCodesByUserId(userId);
        validateUpdateScope(operatorRoles, operatorId, targetUser, targetRoleCodes);

        List<Long> roleIds = new LinkedHashSet<>(dto.getRoleIds()).stream().toList();
        List<SysRole> validRoles = findValidRoles(roleIds);
        validateAllRolesExist(roleIds, validRoles, operatorUser == null ? "unknown" : operatorUser.getUsername(),
                targetUser.getUsername(), "Update user");

        boolean rootSuperAdmin = isRootSuperAdmin(targetUser, targetRoleCodes);
        if (rootSuperAdmin) {
            List<Long> currentRoleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
            if (!new LinkedHashSet<>(currentRoleIds).equals(new LinkedHashSet<>(roleIds))) {
                rejectUserUpdate(operatorId, userId, "根 SUPER_ADMIN 的角色不可修改");
            }
            if (!Objects.equals(targetUser.getStatus(), dto.getStatus())) {
                rejectUserUpdate(operatorId, userId, "根 SUPER_ADMIN 的状态不可修改");
            }
        } else {
            validateAssignableRoles(operatorUser, validRoles,
                    operatorUser == null ? "unknown" : operatorUser.getUsername(), targetUser.getUsername(),
                    "Update user");
        }

        // 所有授权与角色合法性检查完成后，才开始写入，避免越权请求产生部分更新。
        targetUser.setRealName(dto.getRealName().trim());
        targetUser.setMobile(dto.getPhone() == null || dto.getPhone().isBlank() ? null : dto.getPhone().trim());
        targetUser.setStatus(dto.getStatus());
        targetUser.setUpdatedBy(operatorId);
        targetUser.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(targetUser);

        if (!rootSuperAdmin) {
            sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                    .eq(SysUserRole::getUserId, userId));
            LocalDateTime now = LocalDateTime.now();
            for (Long roleId : roleIds) {
                SysUserRole relation = new SysUserRole();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                relation.setCreatedAt(now);
                sysUserRoleMapper.insert(relation);
            }
        }

        log.info("Update user success, operatorUserId={}, targetUserId={}", operatorId, userId);
    }

    private SecurityUser currentSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof SecurityUser securityUser
                ? securityUser
                : null;
    }

    private void validateUpdateScope(
            List<String> operatorRoles,
            Long operatorId,
            SysUser targetUser,
            List<String> targetRoleCodes
    ) {
        if (operatorRoles.contains(SUPER_ADMIN)) {
            return;
        }
        if (operatorRoles.contains(SYSTEM_ADMIN)
                && targetRoleCodes.stream().noneMatch(SYSTEM_ROLE_CODES::contains)) {
            return;
        }
        rejectUserUpdate(operatorId, targetUser.getId(), "当前用户无权编辑目标用户");
    }

    private boolean isRootSuperAdmin(SysUser targetUser, List<String> targetRoleCodes) {
        // 当前表结构没有 root/system 标识，使用初始化账号名与 SUPER_ADMIN 角色联合识别，避免把所有超级管理员都视为根用户。
        return ROOT_ADMIN_USERNAME.equals(targetUser.getUsername()) && targetRoleCodes.contains(SUPER_ADMIN);
    }

    private void rejectUserUpdate(Long operatorId, Long targetUserId, String reason) {
        log.warn("Update user rejected, operatorUserId={}, targetUserId={}, reason={}",
                operatorId, targetUserId, reason);
        throw new ForbiddenUserUpdateException(reason);
    }

    private List<SysRole> findValidRoles(List<Long> roleIds) {
        return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .eq(SysRole::getDeleted, 0));
    }

    private void validateAllRolesExist(
            List<Long> roleIds,
            List<SysRole> validRoles,
            String operator,
            String username,
            String action
    ) {
        Set<Long> validRoleIds = validRoles.stream().map(SysRole::getId).collect(Collectors.toSet());
        List<Long> invalidRoleIds = roleIds.stream().filter(id -> !validRoleIds.contains(id)).toList();
        if (!invalidRoleIds.isEmpty()) {
            log.warn("{} rejected: invalid roles, operator={}, username={}, invalidRoleIds={}",
                    action, operator, username, invalidRoleIds);
            throw new InvalidUserRoleException(invalidRoleIds);
        }
    }

    private void validateAssignableRoles(
            SecurityUser operatorUser,
            List<SysRole> targetRoles,
            String operator,
            String username,
            String action
    ) {
        if (operatorUser == null || operatorUser.getRoles().contains(SUPER_ADMIN)) {
            return;
        }
        if (!operatorUser.getRoles().contains(SYSTEM_ADMIN)) {
            return;
        }

        List<String> forbiddenRoleCodes = targetRoles.stream()
                .map(SysRole::getRoleCode)
                .filter(SYSTEM_ROLE_CODES::contains)
                .distinct()
                .toList();
        if (!forbiddenRoleCodes.isEmpty()) {
            log.warn(
                    "{} rejected: role assignment out of scope, operator={}, username={}, forbiddenRoleCodes={}",
                    action, operator, username, forbiddenRoleCodes
            );
            throw new ForbiddenRoleAssignmentException(forbiddenRoleCodes);
        }
    }
    
}
