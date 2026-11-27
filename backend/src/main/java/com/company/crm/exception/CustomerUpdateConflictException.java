package com.company.crm.exception;

public class CustomerUpdateConflictException extends RuntimeException {
    public CustomerUpdateConflictException() { super("客户资料已变化，请重新加载后编辑"); }
}
