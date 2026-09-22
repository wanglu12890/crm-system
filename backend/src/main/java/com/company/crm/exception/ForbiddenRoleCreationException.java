package com.company.crm.exception;

public class ForbiddenRoleCreationException extends RuntimeException {

    public ForbiddenRoleCreationException(String roleCode) {
        super("当前用户无权创建角色 " + roleCode);
    }
}
