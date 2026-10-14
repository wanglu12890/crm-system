package com.company.crm.exception;

import com.company.crm.controller.AiAnalysisController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiAnalysisController.class)
@Slf4j
public class AiAnalysisExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        return problem(HttpStatus.BAD_REQUEST, "AI分析请求参数不合法", "AI_REQUEST_INVALID");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "AI分析请求格式不合法", "AI_REQUEST_INVALID");
    }

    @ExceptionHandler(AiAuthenticationContextException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationContext(AiAuthenticationContextException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), "AI_AUTHENTICATION_REQUIRED");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "当前用户无权执行AI分析", "FORBIDDEN");
    }

    @ExceptionHandler(DataScopeConfigurationException.class)
    public ResponseEntity<ProblemDetail> handleDataScope(DataScopeConfigurationException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage(), "DATA_SCOPE_CONFIGURATION_ERROR");
    }

    @ExceptionHandler(AiToolArgumentException.class)
    public ResponseEntity<ProblemDetail> handleToolArgument(AiToolArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), "AI_TOOL_ARGUMENT_INVALID");
    }

    @ExceptionHandler(AiToolInvocationException.class)
    public ResponseEntity<ProblemDetail> handleToolInvocation(AiToolInvocationException exception) {
        return problem(HttpStatus.CONFLICT, "客户统计工具不能重复调用", "AI_TOOL_REPEATED");
    }

    @ExceptionHandler(AiAnalysisNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleNotReady(AiAnalysisNotReadyException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), "AI_ANALYSIS_NOT_READY");
    }

    @ExceptionHandler(CustomerStatisticsDataException.class)
    public ResponseEntity<ProblemDetail> handleStatisticsData(CustomerStatisticsDataException exception) {
        log.error("AI customer statistics failed, failureType={}", exception.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "客户统计暂时不可用", "AI_STATISTICS_FAILED");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("AI analysis failed, failureType={}", exception.getClass().getSimpleName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "AI分析服务暂时不可用", "AI_ANALYSIS_FAILED");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("AI分析失败");
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(AiUnsupportedAnalysisException.class)
    public ResponseEntity<ProblemDetail> handleUnsupported(AiUnsupportedAnalysisException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), "AI_ANALYSIS_UNSUPPORTED");
    }

    @ExceptionHandler(AiAnalysisProtocolException.class)
    public ResponseEntity<ProblemDetail> handleProtocol(AiAnalysisProtocolException exception) {
        return problem(HttpStatus.BAD_GATEWAY, exception.getMessage(), "AI_MODEL_RESPONSE_INVALID");
    }

    @ExceptionHandler(AiModelUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleModelUnavailable(AiModelUnavailableException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), "AI_MODEL_UNAVAILABLE");
    }
}
