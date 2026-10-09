package com.company.crm.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldCreateNamedChatClientOnlyWhenAiProfileIsActive() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.build()).thenReturn(chatClient);

        contextRunner
                .withPropertyValues("spring.profiles.active=ai")
                .withBean(ChatClient.Builder.class, () -> builder)
                .withUserConfiguration(AiChatClientConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("qwenChatClient");
                    assertThat(context.getBean("qwenChatClient")).isSameAs(chatClient);
                });
    }

    @Test
    void shouldNotCreateChatClientWhenAiProfileIsInactive() {
        contextRunner
                .withBean(ChatClient.Builder.class, () -> mock(ChatClient.Builder.class))
                .withUserConfiguration(AiChatClientConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean("qwenChatClient"));
    }
}

