package com.company.crm.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Component
@ConfigurationProperties(prefix="crm.ai.risk.advice")
public class RiskAdviceProperties {
    private int maxCustomers=50;
    private int maxInputBytes=12000;
    private int maxOutputChars=24000;
    private int maxConcurrent=4;
    private long timeoutMillis=8000;
    private long requestBudgetMillis=55000;
    public boolean valid() {
        return maxCustomers>0 && maxCustomers<=50 && maxInputBytes>0 && maxInputBytes<=48000
            && maxOutputChars>0 && maxOutputChars<=64000 && maxConcurrent>0 && maxConcurrent<=32
            && timeoutMillis>0 && timeoutMillis<=10000 && requestBudgetMillis>0 && requestBudgetMillis<=85000;
    }
}
