package com.company.crm.exception;

/** A model-controlled tool argument is absent or outside the supported whitelist. */
public class AiToolArgumentException extends RuntimeException {
    public AiToolArgumentException(String message) {
        super(message);
    }
}
