package com.company.crm.ai.tool;

import com.company.crm.dto.risk.*;
import com.company.crm.exception.AiToolInvocationException;
import com.company.crm.service.CustomerRiskService;
import org.springframework.ai.tool.annotation.Tool;
import java.util.concurrent.atomic.AtomicReference;

/** Request-local single-use adapter. Identity and query are never model arguments. */
public final class CustomerRiskTool {
    public enum State { NOT_EXECUTED, RUNNING, SUCCEEDED, FAILED }
    private final AtomicReference<State> state = new AtomicReference<>(State.NOT_EXECUTED);
    private final Long userId;
    private final CustomerRiskQuery query;
    private final CustomerRiskAccessContext expected;
    private final CustomerRiskService risks;

    CustomerRiskTool(Long userId, CustomerRiskQuery query, CustomerRiskAccessContext expected, CustomerRiskService risks) {
        this.userId = userId; this.query = query; this.expected = expected; this.risks = risks;
    }

    @Tool(name = "getCustomerRiskAnalysis", description = "执行Java已验证和绑定的当前请求风险查询，参数必须为空对象")
    public CustomerRiskPage getCustomerRiskAnalysis() {
        if (!state.compareAndSet(State.NOT_EXECUTED, State.RUNNING))
            throw new AiToolInvocationException("每次风险分析最多执行一次工具");
        try {
            var result = risks.analyze(userId, query, expected);
            if (result == null) throw new com.company.crm.exception.CustomerRiskException(
                    com.company.crm.exception.CustomerRiskException.Code.INVALID_DATA);
            state.set(State.SUCCEEDED);
            return result;
        } catch (RuntimeException exception) {
            state.set(State.FAILED);
            throw exception;
        }
    }
    public State getState() { return state.get(); }
}
