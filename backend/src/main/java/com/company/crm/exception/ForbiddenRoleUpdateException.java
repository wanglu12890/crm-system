package com.company.crm.exception;

public class ForbiddenRoleUpdateException extends RuntimeException {

    public ForbiddenRoleUpdateException(String message) {
        super(message);
    }
}
