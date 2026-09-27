package com.company.crm.vo.user;

public record UserListVO(String id, String username, String name, String roleIds, String roleCodes, Boolean rootUser,
                         String phone, String status, String createTime) {
}
