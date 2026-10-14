package com.company.crm.exception;

/** A request-isolated tool was invoked more than the V1 policy permits. */
public class AiToolInvocationException extends RuntimeException {
    public AiToolInvocationException(String message) {
        super(message);
    }
}
