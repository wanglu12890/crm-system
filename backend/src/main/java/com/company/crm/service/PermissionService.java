package com.company.crm.service;

import java.util.List;

import com.company.crm.vo.permission.PermissionTreeVO;

public interface PermissionService {
    
    List<PermissionTreeVO> getPermissionTree();
}
