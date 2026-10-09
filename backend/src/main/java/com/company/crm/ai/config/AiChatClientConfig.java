package com.company.crm.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * AI Profile 下的 ChatClient 基础配置。
 *
 * <p>模型地址、密钥和模型名称交给 Spring AI 自动配置管理；这里仅构建客户端，
 * 不在应用启动阶段发起任何付费模型调用。</p>
 */
@Configuration
@Profile("ai")
public class AiChatClientConfig {

    @Bean("qwenChatClient")
    public ChatClient qwenChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}

