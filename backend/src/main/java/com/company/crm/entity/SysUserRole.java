package com.company.crm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_role")
public class SysUserRole {

    private Long userId;

    private Long roleId;

    private LocalDateTime createdAt;
}
