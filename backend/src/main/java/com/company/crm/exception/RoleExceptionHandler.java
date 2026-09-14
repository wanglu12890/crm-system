package com.company.crm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RoleExceptionHandler {

    @ExceptionHandler(DuplicateRoleCodeException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateRoleCode(DuplicateRoleCodeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("角色创建失败");
        problem.setProperty("code", "ROLE_CODE_ALREADY_EXISTS");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleRoleNotFound(RoleNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "角色权限保存失败", exception.getMessage(), "ROLE_NOT_FOUND");
    }

    @ExceptionHandler(InvalidRolePermissionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRolePermission(InvalidRolePermissionException exception) {
        return problem(HttpStatus.BAD_REQUEST, "角色权限保存失败", exception.getMessage(), "INVALID_PERMISSION");
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String title, String detail, String code
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
