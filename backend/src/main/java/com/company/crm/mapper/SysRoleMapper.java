package com.company.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.crm.entity.SysRole;
import com.company.crm.vo.role.RoleListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 一次查询获得角色信息和权限数量，避免逐角色 COUNT 造成 N+1 查询。
     * LEFT JOIN 会保留没有权限关联的角色，此时 COUNT(rp.permission_id) 返回 0。
     */
    @Select("""
            SELECT r.id,
                   r.role_name AS roleName,
                   r.role_code AS roleCode,
                   r.status,
                   COUNT(rp.permission_id) AS permissionCount,
                   r.remark
              FROM sys_role r
              LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
             WHERE r.deleted = 0
             GROUP BY r.id, r.role_name, r.role_code, r.status, r.remark
             ORDER BY r.id ASC
            """)
    List<RoleListVO> selectRoleList();

    @Select("""
            SELECT role_code
              FROM sys_role
             WHERE id = #{roleId}
               AND deleted = 0
            """)
    String selectRoleCodeById(@Param("roleId") Long roleId);

    /**
     * Only roles that currently contribute the requested enabled permission may
     * participate in data-scope resolution.
     */
    @Select("""
            SELECT DISTINCT r.data_scope
              FROM sys_user_role ur
              JOIN sys_role r
                ON r.id = ur.role_id
               AND r.status = 1
               AND r.deleted = 0
              JOIN sys_role_permission rp ON rp.role_id = r.id
              JOIN sys_permission p
                ON p.id = rp.permission_id
               AND p.status = 1
             WHERE ur.user_id = #{userId}
               AND p.permission_code = #{permissionCode}
            """)
    List<String> selectGrantedDataScopes(
            @Param("userId") Long userId,
            @Param("permissionCode") String permissionCode
    );
}
