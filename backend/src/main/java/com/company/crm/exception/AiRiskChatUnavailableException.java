package com.company.crm.exception;

public class AiRiskChatUnavailableException extends RuntimeException {
    private final boolean enabled;
    public AiRiskChatUnavailableException(boolean enabled) {
        super(enabled ? "当前风险聊天分析尚未就绪" : "当前风险聊天分析尚未开放");
        this.enabled = enabled;
    }
    public boolean isEnabled() { return enabled; }
}
