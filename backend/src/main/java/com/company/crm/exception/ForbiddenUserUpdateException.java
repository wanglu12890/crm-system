package com.company.crm.exception;

public class ForbiddenUserUpdateException extends RuntimeException {

    public ForbiddenUserUpdateException(String reason) {
        super(reason);
    }
}
