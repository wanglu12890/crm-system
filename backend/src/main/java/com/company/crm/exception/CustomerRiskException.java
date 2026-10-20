package com.company.crm.exception;

public class CustomerRiskException extends RuntimeException {
    public enum Code { ACCESS_DENIED, INVALID_QUERY, NOT_READY, NOT_FOUND, CAPACITY_EXCEEDED, INVALID_DATA }
    private final Code code;
    public CustomerRiskException(Code code) { super("Customer risk: " + code.name()); this.code = code; }
    public Code getCode() { return code; }
}
