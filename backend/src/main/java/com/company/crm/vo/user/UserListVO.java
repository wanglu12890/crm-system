package com.company.crm.vo.user;

public record UserListVO(String id, String username, String name, String roleIds,
                         String phone, String status, String createTime) {
}