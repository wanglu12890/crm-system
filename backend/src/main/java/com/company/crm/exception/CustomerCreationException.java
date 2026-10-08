package com.company.crm.exception;

public class CustomerCreationException extends RuntimeException {

    public CustomerCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomerCreationException(String message) {
        super(message);
    }
}
