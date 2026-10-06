package com.company.crm.vo.permission;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 权限管理列表的平铺视图，独立于角色权限配置使用的树形 VO。
 */
@Data
public class PermissionListVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String permissionName;

    private String permissionCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long moduleId;

    private String moduleName;

    private String permissionType;

    private Integer status;

    private String routePath;

    private String httpMethod;

    private String apiPath;

    private Integer sortOrder;
}
