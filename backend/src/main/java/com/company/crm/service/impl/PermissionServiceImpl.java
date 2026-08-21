package com.company.crm.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.crm.service.PermissionService;
import com.company.crm.vo.permission.PermissionTreeVO;

import lombok.RequiredArgsConstructor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.crm.entity.SysPermission;
import com.company.crm.mapper.SysPermissionMapper;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService{

    private final SysPermissionMapper sysPermissionMapper;
    
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
    
}
