package com.company.crm.ai.smoke;

import com.company.crm.ai.config.AiChatClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实模型测试必须同时显式设置 RUN_QWEN_INTEGRATION=true 和 DASHSCOPE_API_KEY。
 * 普通 mvn test 不会调用模型，避免 CI 或日常构建产生外部调用和费用。
 */
@SpringBootTest(classes = QwenLiveIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("ai")
@EnabledIfEnvironmentVariable(named = "RUN_QWEN_INTEGRATION", matches = "(?i:true)")
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
class QwenLiveIntegrationTest {

    private final QwenSmokeService smokeService;

    @Autowired
    QwenLiveIntegrationTest(QwenSmokeService smokeService) {
        this.smokeService = smokeService;
    }

    @Test
    void shouldCompleteTextAndRealToolCallingAgainstQwen() {
        String text = smokeService.chat("请用一句话介绍 CRM 系统的作用。");
        QwenSmokeService.ToolCallResult toolResult = smokeService.chatWithSystemInfoTool(
                "请务必调用 getCrmSystemInfo 工具，告诉我当前系统的名称和系统类型。"
        );

        assertThat(text).isNotBlank();
        assertThat(toolResult.content()).isNotBlank();
        assertThat(toolResult.toolInvocationCount()).isGreaterThan(0);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({AiChatClientConfig.class, QwenSmokeTool.class, QwenSmokeService.class})
    static class TestApplication {
    }
}
