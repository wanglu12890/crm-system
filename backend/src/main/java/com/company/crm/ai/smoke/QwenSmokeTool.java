package com.company.crm.ai.smoke;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring AI Tool Calling 的最小验证工具，只返回固定、非敏感的系统信息。
 */
@Component
@Profile("ai")
public class QwenSmokeTool {

    private final AtomicLong invocationCount = new AtomicLong();

    @Tool(name = "getCrmSystemInfo", description = "获取当前 CRM 系统的名称、系统类型和 AI 集成方式")
    public CrmSystemInfo getCrmSystemInfo() {
        invocationCount.incrementAndGet();
        return new CrmSystemInfo(
                "CRM",
                "Customer Relationship Management",
                "Spring AI"
        );
    }

    /**
     * 提供不含业务数据的结构化执行证据，供显式冒烟测试核验 Java Tool 是否真实执行。
     */
    public long getInvocationCount() {
        return invocationCount.get();
    }

    public record CrmSystemInfo(String systemName, String systemType, String aiIntegration) {
    }
}

