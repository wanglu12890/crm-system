package com.company.crm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateUsername(DuplicateUsernameException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), "USERNAME_ALREADY_EXISTS");
    }

    @ExceptionHandler(InvalidUserRoleException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRole(InvalidUserRoleException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), "INVALID_ROLE");
    }

    @ExceptionHandler(ForbiddenRoleAssignmentException.class)
    public ResponseEntity<ProblemDetail> handleForbiddenRoleAssignment(
            ForbiddenRoleAssignmentException exception
    ) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), "ROLE_ASSIGNMENT_FORBIDDEN");
    }

    @ExceptionHandler(ForbiddenUserUpdateException.class)
    public ResponseEntity<ProblemDetail> handleForbiddenUserUpdate(ForbiddenUserUpdateException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), "USER_UPDATE_FORBIDDEN", "用户编辑失败");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), "USER_NOT_FOUND", "用户编辑失败");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("用户创建失败");
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail,
            String code,
            String title
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
