package com.company.crm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomerExceptionHandler {

    @ExceptionHandler(DataScopeConfigurationException.class)
    public ResponseEntity<ProblemDetail> handleDataScopeConfiguration(
            DataScopeConfigurationException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("客户数据范围配置错误");
        problem.setProperty("code", "DATA_SCOPE_CONFIGURATION_ERROR");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}
