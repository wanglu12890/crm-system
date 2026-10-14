package com.company.crm.exception;

/** The authenticated request did not provide a usable trusted CRM user id. */
public class AiAuthenticationContextException extends RuntimeException {
    public AiAuthenticationContextException() {
        super("当前请求缺少有效认证用户");
    }
}
