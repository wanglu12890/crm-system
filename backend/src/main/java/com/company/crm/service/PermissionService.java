package com.company.crm.service;

import java.util.List;

import com.company.crm.vo.permission.PermissionTreeVO;
import com.company.crm.vo.permission.PermissionListVO;
import com.company.crm.vo.permission.PermissionOverviewVO;

public interface PermissionService {
    
    List<PermissionTreeVO> getPermissionTree();

    List<PermissionListVO> getPermissionList(String keyword, Long moduleId, Integer status);

    PermissionOverviewVO getPermissionOverview();
}
