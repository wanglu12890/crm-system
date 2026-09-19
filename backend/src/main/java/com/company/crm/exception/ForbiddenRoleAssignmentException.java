package com.company.crm.exception;

import java.util.List;

public class ForbiddenRoleAssignmentException extends RuntimeException {

    public ForbiddenRoleAssignmentException(List<String> forbiddenRoleCodes) {
        super("当前用户无权分配目标角色: " + forbiddenRoleCodes);
    }
}
