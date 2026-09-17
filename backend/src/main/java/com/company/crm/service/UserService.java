package com.company.crm.service;

import com.company.crm.dto.user.CreateUserDTO;
import com.company.crm.vo.user.UserListVO;
import java.util.List;

public interface UserService {
    List<UserListVO> listUsers();

    Long createUser(CreateUserDTO dto);
}
