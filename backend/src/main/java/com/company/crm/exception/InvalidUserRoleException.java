package com.company.crm.exception;

import java.util.List;

public class InvalidUserRoleException extends RuntimeException {

    public InvalidUserRoleException(List<Long> invalidRoleIds) {
        super("包含不存在、停用或已删除的角色: " + invalidRoleIds);
    }
}
