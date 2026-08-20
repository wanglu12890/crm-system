package com.company.crm.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

// SecurityUser.java 是一个实现了 Spring Security UserDetails 接口的安全用户对象。
// 它用于将业务系统中的 SysUser（数据库用户对象）转换成 Spring Security 可以识别的认证对象，使 Spring Security 能够完成用户名、密码、状态、权限等认证判断。
public final class SecurityUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String password;
    private final String realName;
    private final Integer status;
    private final Integer deleted;
    private final List<String> roles;
    private final List<GrantedAuthority> authorities;

    public SecurityUser(
            Long userId,
            String username,
            String password,
            String realName,
            Integer status,
            Integer deleted,
            List<String> roles,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.status = status;
        this.deleted = deleted;
        this.roles = List.copyOf(roles);
        this.authorities = List.copyOf(authorities);
    }

    public Long getUserId() {
        return userId;
    }

    public String getRealName() {
        return realName;
    }

    public List<String> getRoles(){
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Objects.equals(status, 1) && Objects.equals(deleted, 0);
    }
}
