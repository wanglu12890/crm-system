package com.company.crm.service;

import com.company.crm.dto.ai.internal.AiAnalysisPlan;
import com.company.crm.dto.risk.CustomerRiskQuery;
import com.company.crm.exception.AiAnalysisProtocolException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiRiskIntentRouterTest {
    @SuppressWarnings("unchecked")
    AiAnalysisPlan classify(String content) {
        var model=mock(ChatModel.class);
        ObjectProvider<ChatClient> clients=mock(ObjectProvider.class);
        var client = ChatClient.create(model);
        when(clients.getIfAvailable()).thenReturn(client);
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(content)))));
        return new AiIntentRouter(clients,new ObjectMapper()).route("合成风险请求");
    }
    @Test void parsesExplicitModeAndCustomerLevelsWithoutTrustingIdentity() {
        var plan=(AiAnalysisPlan.Risk)classify("{\"businessType\":\"RISK\",\"riskIntent\":\"LIST\",\"queryScopeIntent\":\"SELF\",\"queryMode\":\"ALL_STATES\",\"customerLevels\":[\"A\",\"UNRATED\"]}");
        assertThat(plan.queryMode()).isEqualTo(CustomerRiskQuery.Mode.ALL_STATES);
        assertThat(plan.customerLevels()).containsExactlyInAnyOrder("A","UNRATED");
        assertThatThrownBy(()->plan.customerLevels().add("B")).isInstanceOf(UnsupportedOperationException.class);
    }
    @ParameterizedTest
    @ValueSource(strings={"[]","\"A\"","[null]","[1]","[\"HIGH\"]","[\"A\",\"A\"]","{}"})
    void invalidLevelCandidatesAreProtocolFailures(String levels) {
        assertThatThrownBy(()->classify("{\"businessType\":\"RISK\",\"riskIntent\":\"LIST\",\"queryScopeIntent\":\"SELF\",\"queryMode\":\"PRIORITY\",\"customerLevels\":"+levels+"}"))
            .isInstanceOf(AiAnalysisProtocolException.class);
    }
    @ParameterizedTest
    @ValueSource(strings={"{\"businessType\":\"RISK\",\"riskIntent\":\"LIST\",\"queryScopeIntent\":\"SELF\"}",
        "{\"businessType\":\"RISK\",\"riskIntent\":\"LIST\",\"queryScopeIntent\":\"SELF\",\"queryMode\":\"HIGH\",\"customerLevels\":null}",
        "{\"businessType\":\"RISK\",\"riskIntent\":\"LIST\",\"queryScopeIntent\":\"SELF\",\"queryMode\":\"PRIORITY\",\"customerLevels\":null,\"customerId\":\"7\"}",
        "{\"businessType\":\"RISK\",\"riskIntent\":\"LIST\",\"queryScopeIntent\":\"SELF\",\"queryMode\":\"PRIORITY\",\"customerLevels\":null,\"customerLevels\":null}"})
    void expandedRiskProtocolStillRejectsMissingOrForgedFields(String content) {
        assertThatThrownBy(()->classify(content)).isInstanceOf(AiAnalysisProtocolException.class);
    }
}
