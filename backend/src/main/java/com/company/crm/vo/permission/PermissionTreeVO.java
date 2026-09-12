package com.company.crm.vo.permission;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PermissionTreeVO {
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private String routePath;

    private Integer sortOrder;

    private List<PermissionTreeVO> children = new ArrayList<>();
}
