package com.company.crm.vo.auth;

public record TokenVO(
                String accessToken,
                long expiresIn) {

    @Override
    public String toString() {
        return "TokenVO[accessToken=***, expiresIn=" + expiresIn + "]";
    }
}
