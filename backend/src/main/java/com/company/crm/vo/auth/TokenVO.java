package com.company.crm.vo.auth;

public record TokenVO(
                String accessToken,
                long expiresIn) {
}
