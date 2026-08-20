package com.company.crm.security;

import com.company.crm.entity.SysUser;
import com.company.crm.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

// @Service是框架注解，标记该类为 Spring 的 Service 层组件，让 Spring 在启动时自动扫描并创建该类的 Bean 实例，放入 IoC 容器管理
@Service 
// @RequiredArgsConstructor —— 代码生成注解，Lombok 在编译时自动生成一个包含所有 final 字段的构造方法
@RequiredArgsConstructor
// 这个类告诉 Spring Security：用户数据从哪里查。
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!StringUtils.hasText(username)) { 
            throw new UsernameNotFoundException("用户名不能为空");
        }

        SysUser user = sysUserMapper.selectByUsername(username.trim()); // 查询
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (!Objects.equals(user.getDeleted(), 0)) {
            throw new DisabledException("用户已删除，禁止登录");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new DisabledException("用户状态异常，禁止登录");
        }

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());

        // System.out.println("CustomUserDetailsService roles = " + roles);

        // Role and permission authorities will be loaded in the authorization phase.
        // 如果顺利查到该用户，则转换成一个新的对象 SecurityUser，并返回
        List<SimpleGrantedAuthority> authorities = List.of(); 
        return new SecurityUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRealName(),
                user.getStatus(),
                user.getDeleted(),
                roles,
                authorities
        );
    }
}
