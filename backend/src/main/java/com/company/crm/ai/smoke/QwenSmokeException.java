package com.company.crm.ai.smoke;

/**
 * Qwen 基础连通性或 Tool Calling 冒烟验证失败时抛出的统一异常。
 */
public class QwenSmokeException extends RuntimeException {

    public QwenSmokeException(String message) {
        super(message);
    }

    public QwenSmokeException(String message, Throwable cause) {
        super(message, cause);
    }
}

