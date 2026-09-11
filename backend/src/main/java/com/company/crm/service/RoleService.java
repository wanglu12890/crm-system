package com.company.crm.service;

import com.company.crm.dto.role.CreateRoleDTO;
import com.company.crm.vo.role.RoleListVO;

import java.util.List;

public interface RoleService {

    List<RoleListVO> listRoles();

    Long createRole(CreateRoleDTO dto);
}
