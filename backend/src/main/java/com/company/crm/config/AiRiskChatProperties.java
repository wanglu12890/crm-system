package com.company.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Only gates natural-language risk chat; structured S2-A remains independent. */
@Data
@Component
@ConfigurationProperties(prefix = "crm.ai.risk.chat")
public class AiRiskChatProperties {
    private boolean enabled = false;
}
