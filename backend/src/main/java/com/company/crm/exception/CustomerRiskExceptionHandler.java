package com.company.crm.exception;

import com.company.crm.controller.CustomerRiskController;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = CustomerRiskController.class)
public class CustomerRiskExceptionHandler {
    @ExceptionHandler(CustomerRiskException.class)
    public ResponseEntity<ProblemDetail> risk(CustomerRiskException exception) {
        return response(exception);
    }

    /** Shared status/code mapping; both controller-scoped advices delegate here. */
    public static ResponseEntity<ProblemDetail> response(CustomerRiskException exception) {
        return switch (exception.getCode()) {
            case INVALID_QUERY -> problem(400, "RISK_REQUEST_INVALID", "风险分析请求不合法");
            case AUTHENTICATION_REQUIRED -> problem(401, "RISK_AUTHENTICATION_REQUIRED", "请先登录");
            case ACCESS_DENIED -> problem(403, "RISK_ACCESS_DENIED", "无权执行该风险分析");
            case SCOPE_UNSUPPORTED -> problem(422, "RISK_SCOPE_UNSUPPORTED", "请求范围不受支持");
            case NOT_FOUND -> problem(404, "RISK_CUSTOMER_NOT_FOUND", "客户不存在、不可见或不适用");
            case RESULT_STALE -> problem(409, "RISK_RESULT_STALE", "分析结果已变化，请刷新");
            case ACCESS_CONTEXT_CHANGED -> problem(403, "RISK_ACCESS_CONTEXT_CHANGED", "访问上下文已变化，请重新分析");
            case QUERY_UNSUPPORTED -> problem(422, "RISK_QUERY_UNSUPPORTED", "风险查询条件不受支持");
            case CUSTOMER_SELECTION_REQUIRED -> problem(422, "RISK_CUSTOMER_SELECTION_REQUIRED", "请明确选择客户后查看原因");
            case NOT_READY -> problem(503, "RISK_RULES_NOT_READY", "关注规则暂不可用");
            case CAPACITY_EXCEEDED -> problem(503, "RISK_CAPACITY_EXCEEDED", "分析范围超过处理容量");
            case INVALID_DATA -> problem(500, "RISK_QUERY_FAILED", "风险分析暂不可用");
        };
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException e) {
        return problem(400, "RISK_REQUEST_INVALID", "风险分析请求格式不合法");
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> denied(AccessDeniedException e) {
        return problem(403, "RISK_ACCESS_DENIED", "无权执行该风险分析");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception e) {
        return problem(500, "RISK_ANALYSIS_FAILED", "风险分析暂不可用");
    }
    private static ResponseEntity<ProblemDetail> problem(int status, String code, String message) {
        var body = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), message);
        body.setTitle("客户关注分析失败");
        body.setProperty("code", code);
        return ResponseEntity.status(status).body(body);
    }
}
