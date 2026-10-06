package com.company.crm.exception;

/** 当前操作人不允许通过管理员入口重置目标用户密码。 */
public class ForbiddenPasswordResetException extends RuntimeException {

    public ForbiddenPasswordResetException(String reason) {
        super(reason);
    }
}
