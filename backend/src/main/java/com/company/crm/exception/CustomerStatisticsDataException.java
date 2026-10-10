package com.company.crm.exception;

/** Raised when persisted customer levels make the aggregate internally inconsistent. */
public class CustomerStatisticsDataException extends RuntimeException {
    public CustomerStatisticsDataException(String message) {
        super(message);
    }
}
