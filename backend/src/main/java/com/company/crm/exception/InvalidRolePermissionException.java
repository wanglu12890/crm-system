package com.company.crm.exception;

import java.util.Collection;

public class InvalidRolePermissionException extends RuntimeException {

    public InvalidRolePermissionException(Collection<Long> invalidPermissionIds) {
        super("存在无效或已禁用权限：" + invalidPermissionIds);
    }
}
