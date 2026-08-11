package com.company.crm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 用来处理“已经进入 Controller/Service 之后”发生的认证异常。
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler({ BadCredentialsException.class, UsernameNotFoundException.class })
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(AuthenticationException exception) {
        return unauthorized("用户名或密码错误", "INVALID_CREDENTIALS");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ProblemDetail> handleDisabledAccount(DisabledException exception) {
        return unauthorized(exception.getMessage(), "ACCOUNT_DISABLED");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(AuthenticationException exception) {
        return unauthorized("认证失败", "AUTHENTICATION_FAILED");
    }

    private ResponseEntity<ProblemDetail> unauthorized(String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setTitle("认证失败");
        problem.setProperty("code", code);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
}
