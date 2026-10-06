package com.company.crm.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.company.crm.service.PermissionService;
import com.company.crm.vo.permission.PermissionTreeVO;
import com.company.crm.vo.permission.PermissionListVO;
import com.company.crm.vo.permission.PermissionOverviewVO;

import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.crm.entity.SysPermission;
import com.company.crm.entity.SysRole;
import com.company.crm.entity.SysRolePermission;
import com.company.crm.mapper.SysPermissionMapper;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysRolePermissionMapper;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService{

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<PermissionTreeVO> getPermissionTree() {
        // 1. 查询启用权限
        List<SysPermission> permissions = sysPermissionMapper.selectList(
        Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getParentId)
                .orderByAsc(SysPermission::getSortOrder)
                .orderByAsc(SysPermission::getId)
            );
            
        // 2. Entity 转 VO，同时构造 id -> VO 映射
        List<PermissionTreeVO> nodes = new ArrayList<>();
        Map<Long, PermissionTreeVO> nodeMap = new HashMap<>();
             for (SysPermission permission : permissions) {

                PermissionTreeVO node = new PermissionTreeVO();

                node.setId(permission.getId());
                node.setParentId(permission.getParentId());
                node.setPermissionCode(permission.getPermissionCode());
                node.setPermissionName(permission.getPermissionName());
                node.setPermissionType(permission.getPermissionType());
                node.setRoutePath(permission.getRoutePath());
                node.setSortOrder(permission.getSortOrder());

                nodes.add(node);
                nodeMap.put(node.getId(), node);
        }

        // 3. 保存最终的根节点
        List<PermissionTreeVO> roots = new ArrayList<>();

        // 4. 根据 parentId 建树
        for (PermissionTreeVO node : nodes) {

            if (node.getParentId() == 0L) {
                // parentId = 0，说明这是根节点
                roots.add(node);
            } else {
                // 根据 parentId 找父节点
                PermissionTreeVO parent = nodeMap.get(node.getParentId());

                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        // 5. 返回整棵权限树
        return roots;
        
        
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionListVO> getPermissionList(String keyword, Long moduleId, Integer status) {
        // 权限数量较少，一次读取完整层级后再筛选，避免父节点被 keyword/status 提前过滤而导致模块归属错误。
        List<SysPermission> permissions = loadAllPermissions();
        Map<Long, SysPermission> permissionMap = new HashMap<>();
        permissions.forEach(permission -> permissionMap.put(permission.getId(), permission));

        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;

        List<PermissionListVO> result = new ArrayList<>();
        for (SysPermission permission : permissions) {
            SysPermission module = resolveModule(permission, permissionMap);
            if (!matchesKeyword(permission, normalizedKeyword)
                    || status != null && !Objects.equals(status, permission.getStatus())
                    || moduleId != null && (module == null || !Objects.equals(moduleId, module.getId()))) {
                continue;
            }
            result.add(toPermissionListVO(permission, module));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionOverviewVO getPermissionOverview() {
        List<SysRole> roles = sysRoleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getDeleted, 0)
                        .orderByAsc(SysRole::getId)
        ).stream().filter(role -> Objects.equals(role.getDeleted(), 0)).toList();
        List<SysPermission> permissions = loadAllPermissions();
        List<SysRolePermission> relations = sysRolePermissionMapper.selectList(
                Wrappers.<SysRolePermission>lambdaQuery()
                        .orderByAsc(SysRolePermission::getRoleId)
                        .orderByAsc(SysRolePermission::getPermissionId)
        );

        Map<Long, SysPermission> permissionMap = new HashMap<>();
        permissions.forEach(permission -> permissionMap.put(permission.getId(), permission));

        List<PermissionOverviewVO.RoleItem> roleItems = roles.stream()
                .map(role -> new PermissionOverviewVO.RoleItem(
                        role.getId(), role.getRoleName(), role.getRoleCode(), role.getStatus()))
                .toList();
        List<PermissionOverviewVO.PermissionItem> permissionItems = permissions.stream()
                .map(permission -> {
                    SysPermission module = resolveModule(permission, permissionMap);
                    return new PermissionOverviewVO.PermissionItem(
                            permission.getId(),
                            permission.getParentId(),
                            permission.getPermissionName(),
                            permission.getPermissionCode(),
                            module == null ? null : module.getId(),
                            module == null ? null : module.getPermissionName(),
                            permission.getPermissionType(),
                            permission.getStatus(),
                            permission.getSortOrder()
                    );
                })
                .toList();

        Set<Long> validRoleIds = roles.stream().map(SysRole::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> validPermissionIds = permissions.stream()
                .map(SysPermission::getId)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, LinkedHashSet<String>> relationSets = new LinkedHashMap<>();
        roles.forEach(role -> relationSets.put(String.valueOf(role.getId()), new LinkedHashSet<>()));
        for (SysRolePermission relation : relations) {
            if (validRoleIds.contains(relation.getRoleId())
                    && validPermissionIds.contains(relation.getPermissionId())) {
                relationSets.get(String.valueOf(relation.getRoleId()))
                        .add(String.valueOf(relation.getPermissionId()));
            }
        }
        Map<String, List<String>> rolePermissions = new LinkedHashMap<>();
        relationSets.forEach((roleId, permissionIds) ->
                rolePermissions.put(roleId, new ArrayList<>(permissionIds)));

        int moduleCount = (int) permissionItems.stream()
                .map(PermissionOverviewVO.PermissionItem::getModuleId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        PermissionOverviewVO.Summary summary = new PermissionOverviewVO.Summary(
                permissionItems.size(), roleItems.size(), moduleCount);
        return new PermissionOverviewVO(summary, roleItems, permissionItems, rolePermissions);
    }

    private List<SysPermission> loadAllPermissions() {
        return sysPermissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery()
                        .orderByAsc(SysPermission::getParentId)
                        .orderByAsc(SysPermission::getSortOrder)
                        .orderByAsc(SysPermission::getId)
        );
    }

    /**
     * 模块定义为当前节点向上追溯时遇到的最近 MENU 节点；该节点的父链必须最终到达 parentId=0。
     * 这样按钮/API 归属到具体业务菜单，MENU 本身归属自身；孤儿节点或循环层级不会被错误归类。
     */
    private SysPermission resolveModule(
            SysPermission permission,
            Map<Long, SysPermission> permissionMap
    ) {
        SysPermission current = permission;
        SysPermission nearestMenu = null;
        Set<Long> visitedIds = new HashSet<>();

        while (current != null && visitedIds.add(current.getId())) {
            if (nearestMenu == null && "MENU".equals(current.getPermissionType())) {
                nearestMenu = current;
            }
            if (Objects.equals(current.getParentId(), 0L)) {
                return nearestMenu;
            }
            current = permissionMap.get(current.getParentId());
        }
        return null;
    }

    private boolean matchesKeyword(SysPermission permission, String keyword) {
        if (keyword == null) {
            return true;
        }
        return containsIgnoreCase(permission.getPermissionName(), keyword)
                || containsIgnoreCase(permission.getPermissionCode(), keyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private PermissionListVO toPermissionListVO(SysPermission permission, SysPermission module) {
        PermissionListVO vo = new PermissionListVO();
        vo.setId(permission.getId());
        vo.setParentId(permission.getParentId());
        vo.setPermissionName(permission.getPermissionName());
        vo.setPermissionCode(permission.getPermissionCode());
        vo.setModuleId(module == null ? null : module.getId());
        vo.setModuleName(module == null ? null : module.getPermissionName());
        vo.setPermissionType(permission.getPermissionType());
        vo.setStatus(permission.getStatus());
        vo.setRoutePath(permission.getRoutePath());
        vo.setHttpMethod(permission.getHttpMethod());
        vo.setApiPath(permission.getApiPath());
        vo.setSortOrder(permission.getSortOrder());
        return vo;
    }
    
}
