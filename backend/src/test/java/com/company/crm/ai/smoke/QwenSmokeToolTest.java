package com.company.crm.ai.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class QwenSmokeToolTest {

    @Test
    void shouldExposeDeterministicNonSensitiveSystemInformation() throws NoSuchMethodException {
        QwenSmokeTool tool = new QwenSmokeTool();
        Method method = QwenSmokeTool.class.getMethod("getCrmSystemInfo");

        QwenSmokeTool.CrmSystemInfo result = tool.getCrmSystemInfo();

        assertThat(method.getAnnotation(Tool.class)).isNotNull();
        assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("getCrmSystemInfo");
        assertThat(result.systemName()).isEqualTo("CRM");
        assertThat(result.systemType()).isEqualTo("Customer Relationship Management");
        assertThat(result.aiIntegration()).isEqualTo("Spring AI");
        assertThat(tool.getInvocationCount()).isEqualTo(1);
    }

    @Test
    void shouldRegisterAndExecuteThroughSpringAiToolCallback() {
        QwenSmokeTool tool = new QwenSmokeTool();
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(tool)
                .build()
                .getToolCallbacks();

        assertThat(callbacks).hasSize(1);
        assertThat(callbacks[0].getToolDefinition().name()).isEqualTo("getCrmSystemInfo");

        String callbackResult = callbacks[0].call("{}");

        assertThat(callbackResult).contains("CRM", "Customer Relationship Management", "Spring AI");
        assertThat(tool.getInvocationCount()).isEqualTo(1);
    }
}

