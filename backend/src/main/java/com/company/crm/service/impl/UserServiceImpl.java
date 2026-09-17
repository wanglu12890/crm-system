package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.crm.dto.user.CreateUserDTO;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysUser;
import com.company.crm.entity.SysUserRole;
import com.company.crm.exception.DuplicateUsernameException;
import com.company.crm.exception.InvalidUserRoleException;
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
        Long operatorId = authentication.getPrincipal() instanceof SecurityUser securityUser
                ? securityUser.getUserId()
                : null;
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
    
}
