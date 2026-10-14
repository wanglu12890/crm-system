package com.company.crm.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatClientConfigTest {

    @Test
    void shouldBoundConnectAndReadTimeouts() {
        var builder = mock(org.springframework.web.client.RestClient.Builder.class);
        new AiChatClientConfig().aiHttpTimeouts().customize(builder);
        var captor = org.mockito.ArgumentCaptor.forClass(org.springframework.http.client.ClientHttpRequestFactory.class);
        org.mockito.Mockito.verify(builder).requestFactory(captor.capture());
        var factory = captor.getValue();
        assertThat(factory).isInstanceOf(org.springframework.http.client.JdkClientHttpRequestFactory.class);
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(factory, "readTimeout"))
                .isEqualTo(java.time.Duration.ofSeconds(20));
        var client = (java.net.http.HttpClient) org.springframework.test.util.ReflectionTestUtils.getField(factory, "httpClient");
        assertThat(client.connectTimeout()).contains(java.time.Duration.ofSeconds(5));
    }

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

