package com.company.crm.ai.smoke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QwenSmokeServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private QwenSmokeTool smokeTool;
    private QwenSmokeService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);
        smokeTool = new QwenSmokeTool();
        service = new QwenSmokeService(chatClient, smokeTool);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
    }

    @Test
    void shouldReturnNonEmptyTextResponse() {
        when(responseSpec.content()).thenReturn("CRM 用于管理客户关系。");

        assertThat(service.chat("请介绍 CRM")).isEqualTo("CRM 用于管理客户关系。");
    }

    @Test
    void shouldRejectEmptyModelResponse() {
        when(responseSpec.content()).thenReturn("  ");

        assertThatThrownBy(() -> service.chat("请介绍 CRM"))
                .isInstanceOf(QwenSmokeException.class)
                .hasMessage("Qwen 返回内容为空");
    }

    @Test
    void shouldWrapModelClientFailureWithoutLeakingDetailsInMessage() {
        when(requestSpec.call()).thenThrow(new IllegalStateException("remote detail"));

        assertThatThrownBy(() -> service.chat("请介绍 CRM"))
                .isInstanceOf(QwenSmokeException.class)
                .hasMessage("Qwen 文本调用失败")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectToolResponseWhenJavaToolWasNotExecuted() {
        when(responseSpec.content()).thenReturn("模型直接生成的回答");

        assertThatThrownBy(() -> service.chatWithSystemInfoTool("调用系统信息工具"))
                .isInstanceOf(QwenSmokeException.class)
                .hasMessage("模型未调用 getCrmSystemInfo 工具");
    }
}

