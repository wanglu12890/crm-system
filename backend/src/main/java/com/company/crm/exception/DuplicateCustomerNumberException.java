package com.company.crm.exception;

public class DuplicateCustomerNumberException extends RuntimeException {

    public DuplicateCustomerNumberException() {
        super("客户编号冲突，请重新提交");
    }
}
