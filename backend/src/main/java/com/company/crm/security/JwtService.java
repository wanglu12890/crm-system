package com.company.crm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;


// 定义一个JwtService类，并使用注解 @Service — 标记为 Spring 服务组件
@Service
public class JwtService {

    // 成员变量
    private static final String CLAIM_USER_ID = "userId";  // 定义 JWT 中存储数据的"键名"？？？（√）
    private static final String CLAIM_USERNAME = "username";

    private final JwtProperties properties;  // 存储配置（密钥、过期时间），从 application.yml 读取
    private final SecretKey signingKey;  // 加密签名用的密钥对象，用于生成和验证签名

    // 有参构造函数：方法名与类名相同、无返回值、使用 new 创建对象时自动调用、作用是初始化对象的状态（给字段赋值）
    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // properties.getSecret() 获取 JWT 的密钥，用于① 生成令牌，用密钥对内容进行签名；②验证令牌，用同一个密钥验证签名是否被篡改
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));  
    }

    // 实例方法
    // 生成令牌
    public String generateAccessToken(SecurityUser user) {
        Objects.requireNonNull(user, "user must not be null");

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getExpire());
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getUserId())
                .claim(CLAIM_USERNAME, user.getUsername())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).get(CLAIM_USERNAME, String.class);
    }

    public boolean isTokenValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = parseClaims(token);
            return StringUtils.hasText(claims.get(CLAIM_USERNAME, String.class))
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
