package com.company.crm.vo.permission;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PermissionTreeVO {
    
    private Long id;

    private Long parentId;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private String routePath;

    private Integer sortOrder;

    private List<PermissionTreeVO> children = new ArrayList<>();
}
