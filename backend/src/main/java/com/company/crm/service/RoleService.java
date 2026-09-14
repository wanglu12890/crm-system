package com.company.crm.service;

import com.company.crm.dto.role.CreateRoleDTO;
import com.company.crm.vo.role.RoleListVO;

import java.util.List;

public interface RoleService {

    List<RoleListVO> listRoles();

    List<Long> getRolePermissionIds(Long roleId);

    void updateRolePermissions(Long roleId, List<Long> permissionIds);

    Long createRole(CreateRoleDTO dto);
}
