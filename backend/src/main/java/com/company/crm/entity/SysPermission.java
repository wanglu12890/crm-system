package com.company.crm.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private String routePath;

    private String componentPath;

    private String httpMethod;

    private String apiPath;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
