package com.company.crm.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.retry.support.RetryTemplate;
import java.time.Duration;

/** Dedicated synchronous advice transport: bounded request timeout, no worker threads and no retries. */
@Component
@Profile("ai")
public class RiskAdviceChatClientFactory {
    private final String apiKey, baseUrl, path, model;
    private final java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public RiskAdviceChatClientFactory(@Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.completions-path:/v1/chat/completions}") String path,
            @Value("${spring.ai.openai.chat.options.model:qwen-plus}") String model) {
        this.apiKey=apiKey; this.baseUrl=baseUrl; this.path=path; this.model=model;
    }
    public ChatClient create(Duration timeout) {
        var requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(timeout);
        var api = OpenAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).completionsPath(path)
                .restClientBuilder(RestClient.builder().requestFactory(requests)).build();
        var chat = OpenAiChatModel.builder().openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(0.2).maxTokens(4000).build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).fixedBackoff(1).build()).build();
        return ChatClient.create(chat);
    }
}
