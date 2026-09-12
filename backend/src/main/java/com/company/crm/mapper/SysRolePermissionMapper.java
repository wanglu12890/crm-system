package com.company.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crm.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 仅返回仍处于启用状态的权限，避免关联表中的历史关系回显为有效权限。
     */
    @Select("""
            SELECT rp.permission_id
              FROM sys_role_permission rp
              JOIN sys_permission p ON p.id = rp.permission_id
             WHERE rp.role_id = #{roleId}
               AND p.status = 1
             ORDER BY rp.permission_id ASC
            """)
    List<Long> selectEnabledPermissionIdsByRoleId(Long roleId);
}
