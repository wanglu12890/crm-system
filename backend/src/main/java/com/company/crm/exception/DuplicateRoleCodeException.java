package com.company.crm.exception;

public class DuplicateRoleCodeException extends RuntimeException {

    public DuplicateRoleCodeException(String roleCode) {
        super("角色编码 " + roleCode + " 已存在");
    }
}
