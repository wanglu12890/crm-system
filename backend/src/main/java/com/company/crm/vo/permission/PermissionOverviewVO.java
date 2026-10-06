package com.company.crm.vo.permission;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 权限总览只描述角色与权限的数据库配置关系，不代表某个用户当前的有效授权。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionOverviewVO {

    private Summary summary;

    private List<RoleItem> roles = new ArrayList<>();

    private List<PermissionItem> permissions = new ArrayList<>();

    private Map<String, List<String>> rolePermissions = new LinkedHashMap<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int permissionCount;
        private int roleCount;
        private int moduleCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String roleName;
        private String roleCode;
        private Integer status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionItem {
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
        private Integer sortOrder;
    }
}
