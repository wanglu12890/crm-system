package com.company.crm.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("用户不存在: " + userId);
    }
}
