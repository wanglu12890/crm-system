package com.company.crm.vo.auth;

import java.util.List;

public record CurrentUserVO(
        Long id,
        String username,
        String realName,
        List<String> roles
) {
}
