package com.company.crm.exception;

/** Raised while the business AI orchestration remains intentionally disabled. */
public class AiAnalysisNotReadyException extends RuntimeException {
    public AiAnalysisNotReadyException() {
        super("AI分析功能尚未就绪");
    }
}
