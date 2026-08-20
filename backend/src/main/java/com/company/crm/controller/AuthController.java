package com.company.crm.controller;

import com.company.crm.dto.auth.LoginDTO;
import com.company.crm.security.SecurityUser;
import com.company.crm.service.AuthService;
import com.company.crm.vo.auth.CurrentUserVO;
import com.company.crm.vo.auth.TokenVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证控制器
 * 负责处理登录、登出等认证相关请求
 */
@RestController // 1.标记为 REST 控制器
@RequestMapping("/auth") // 2.所有接口路径前缀 /auth
@RequiredArgsConstructor // 3.自动生成构造方法
public class AuthController {

    private final AuthService authService; // 4. 注入业务服务（final + 构造注入）

    /**
     * 用户登录
     * 
     * @param loginDTO 登录请求参数（用户名+密码）
     * @return TokenVO（JWT令牌+过期时间）
     */
    @PostMapping("/login")  // 5. 映射 POST /auth/login
    public ResponseEntity<TokenVO> login(
        @Valid @RequestBody LoginDTO loginDTO // 6. 校验并绑定请求体
    ) {  
        // 7. 调用业务层处理登录，返回令牌数据
        TokenVO tokenVO = authService.login(loginDTO);
        // 8. 返回 HTTP 200 + 令牌数据
        return ResponseEntity.ok(tokenVO);
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserVO> currentUser(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        
        // System.out.println("=== ENTER /auth/me ===");
        // System.out.println(
        //     "AuthController SecurityUser roles = " + securityUser.getRoles()
        // );

        CurrentUserVO currentUser = new CurrentUserVO(
                securityUser.getUserId(),
                securityUser.getUsername(),
                securityUser.getRealName(),
                securityUser.getRoles()
        );
        return ResponseEntity.ok(currentUser);
    }
}
