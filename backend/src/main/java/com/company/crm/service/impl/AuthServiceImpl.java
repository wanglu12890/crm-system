package com.company.crm.service.impl;

import com.company.crm.dto.auth.LoginDTO;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.security.JwtProperties;
import com.company.crm.security.JwtService;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.AuthService;
import com.company.crm.vo.auth.TokenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import javax.swing.Spring;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SysUserMapper sysUserMapper;

    @Override // 编译检查。确保方法签名正确，能被 Spring 识别为接口实现
    @Transactional(rollbackFor = Exception.class) // 运行管理。Spring 在运行时为这个方法增强事务能力
    public TokenVO login(LoginDTO loginDTO) {
        // 阶段一：把“用户提交了一组用户名+密码，请帮我认证”【登录申请单】包装成 UsernamePasswordAuthenticationToken（Spring Security内部认证对象） 
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                loginDTO.username().trim(),
                loginDTO.password()
        );
        // 阶段二：将 登录申请单 交给 AuthenticationManager 认证，完成将结果交给 authenticationResult，其中包含对象 Principal：securityUser
        Authentication authenticationResult = authenticationManager.authenticate(authenticationRequest);
        // 阶段三： 查看securityUser状态 AuthenticationResult {principal: SecurityUser, auth: true}
        if (!(authenticationResult.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new AuthenticationServiceException("认证主体类型异常");
        }

        String accessToken = jwtService.generateAccessToken(securityUser);
        int updatedRows = sysUserMapper.updateLastLoginAt(securityUser.getUserId(), LocalDateTime.now());
        if (updatedRows != 1) {
            throw new AuthenticationServiceException("用户状态已变更，请重新登录");
        }

        return new TokenVO(accessToken, jwtProperties.getExpire());
    }
}
