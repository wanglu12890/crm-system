package com.company.crm.service;

import com.company.crm.dto.auth.LoginDTO;
import com.company.crm.vo.auth.TokenVO;

public interface AuthService {

    TokenVO login(LoginDTO loginDTO);
}
