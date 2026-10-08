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

    @ExceptionHandler(ForbiddenCustomerCreationException.class)
    public ResponseEntity<ProblemDetail> handleForbiddenCreation(ForbiddenCustomerCreationException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), "CUSTOMER_CREATION_FORBIDDEN");
    }

    @ExceptionHandler(DuplicateCustomerNumberException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateNumber(DuplicateCustomerNumberException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), "CUSTOMER_NUMBER_CONFLICT");
    }

    @ExceptionHandler(CustomerCreationException.class)
    public ResponseEntity<ProblemDetail> handleCreationFailure(CustomerCreationException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), "CUSTOMER_CREATION_FAILED");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("客户创建失败");
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
