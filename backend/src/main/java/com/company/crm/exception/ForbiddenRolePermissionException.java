package com.company.crm.exception;

public class ForbiddenRolePermissionException extends RuntimeException {

    public ForbiddenRolePermissionException() {
        super("不允许修改 SUPER_ADMIN 自身权限");
    }
}
