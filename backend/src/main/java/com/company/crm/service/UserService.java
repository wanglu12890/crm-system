package com.company.crm.service;

import com.company.crm.vo.user.UserListVO;
import java.util.List;

public interface UserService {
    List<UserListVO> listUsers();
}
