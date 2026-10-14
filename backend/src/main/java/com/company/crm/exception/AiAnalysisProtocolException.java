package com.company.crm.exception;

/** Safe, fixed messages only; never include raw model output. */
public class AiAnalysisProtocolException extends RuntimeException {
    public AiAnalysisProtocolException() { super("模型未返回有效的客户统计工具调用，请重试"); }
}
