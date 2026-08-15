package com.company.crm.service.impl;

import com.company.crm.mapper.SysUserMapper;
import com.company.crm.service.UserService;
import com.company.crm.vo.user.UserListVO;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserListVO> listUsers() {
        return sysUserMapper.selectUserList();
    }
    
}
