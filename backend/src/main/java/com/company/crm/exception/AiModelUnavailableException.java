package com.company.crm.exception;

public class AiModelUnavailableException extends RuntimeException {
    public AiModelUnavailableException() { super("AI模型服务暂时不可用，请稍后重试"); }
}
