package com.company.crm.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;

class AiRiskChatPropertiesTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AiRiskChatProperties.class)
    static class PropertiesConfiguration {}

    @Test void defaultsClosedAndCanBeBoundWithoutChangingStructuredRiskRules() {
        var runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);
        runner.run(context -> assertThat(context.getBean(AiRiskChatProperties.class).isEnabled()).isFalse());
        runner.withPropertyValues("crm.ai.risk.chat.enabled=true").run(context ->
                assertThat(context.getBean(AiRiskChatProperties.class).isEnabled()).isTrue());
    }
}
