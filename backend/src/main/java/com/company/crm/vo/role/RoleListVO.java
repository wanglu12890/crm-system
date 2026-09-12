package com.company.crm.vo.role;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 角色列表展示对象，隔离数据库实体与接口响应。
 * permissionCount 是角色权限关联表的统计结果，不属于 SysRole 实体字段。
 */
public record RoleListVO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        String roleName,
        String roleCode,
        Integer status,
        Long permissionCount,
        String remark
) {
}
