package com.company.crm.ai.smoke;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Qwen 文本与 Tool Calling 的最小显式验证入口，不对外暴露 HTTP API。
 */
@Service
@Profile("ai")
public class QwenSmokeService {

    private final ChatClient chatClient;
    private final QwenSmokeTool smokeTool;

    public QwenSmokeService(@Qualifier("qwenChatClient") ChatClient chatClient,
                            QwenSmokeTool smokeTool) {
        this.chatClient = chatClient;
        this.smokeTool = smokeTool;
    }

    public String chat(String prompt) {
        try {
            return requireContent(chatClient.prompt().user(prompt).call().content());
        } catch (QwenSmokeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new QwenSmokeException("Qwen 文本调用失败", exception);
        }
    }

    public ToolCallResult chatWithSystemInfoTool(String prompt) {
        long countBefore = smokeTool.getInvocationCount();
        try {
            String content = requireContent(chatClient.prompt()
                    .user(prompt)
                    .tools(smokeTool)
                    .call()
                    .content());
            long countAfter = smokeTool.getInvocationCount();
            if (countAfter <= countBefore) {
                throw new QwenSmokeException("模型未调用 getCrmSystemInfo 工具");
            }
            return new ToolCallResult(content, countAfter - countBefore);
        } catch (QwenSmokeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new QwenSmokeException("Qwen Tool Calling 调用失败", exception);
        }
    }

    private String requireContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new QwenSmokeException("Qwen 返回内容为空");
        }
        return content;
    }

    public record ToolCallResult(String content, long toolInvocationCount) {
    }
}
