package com.company.crm.exception;

public class ForbiddenCustomerCreationException extends RuntimeException {

    public ForbiddenCustomerCreationException(String message) {
        super(message);
    }
}
