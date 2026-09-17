package com.company.crm.exception;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("用户名 " + username + " 已存在");
    }
}
