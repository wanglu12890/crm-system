package com.company.crm.exception;

/** Effective role/data-scope configuration cannot safely authorize the request. */
public class DataScopeConfigurationException extends RuntimeException {

    public DataScopeConfigurationException(String message) {
        super(message);
    }
}
