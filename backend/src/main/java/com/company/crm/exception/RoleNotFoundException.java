package com.company.crm.exception;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long roleId) {
        super("角色不存在，roleId=" + roleId);
    }
}
