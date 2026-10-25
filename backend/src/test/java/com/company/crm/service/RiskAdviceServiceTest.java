package com.company.crm.service;

import com.company.crm.ai.config.RiskAdviceChatClientFactory;
import com.company.crm.config.RiskAdviceProperties;
import com.company.crm.dto.risk.*;
import com.company.crm.exception.RiskAdviceUnavailableException;
import com.company.crm.service.impl.RiskAdviceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RiskAdviceServiceTest {
    final ChatModel model=mock(ChatModel.class);
    final RiskAdviceChatClientFactory factory=mock(RiskAdviceChatClientFactory.class);
    final RiskAdviceProperties limits=new RiskAdviceProperties();
    final ObjectMapper mapper=new ObjectMapper();
    RiskAdviceServiceImpl service;
    @SuppressWarnings("unchecked") ObjectProvider<RiskAdviceChatClientFactory> provider=mock(ObjectProvider.class);
    static final String VALID="{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}";
    @BeforeEach void setup() throws Exception {
        var client=ChatClient.create(model);
        when(factory.create(any())).thenReturn(client);
        when(provider.getIfAvailable()).thenReturn(factory);
        when(model.call(any(Prompt.class))).thenReturn(text(VALID));
        service=new RiskAdviceServiceImpl(provider,mapper,limits,new ClassPathResource("prompts/customer-risk-advice.st"));
        clearInvocations(model);
    }
    static ChatResponse text(String value) { return new ChatResponse(List.of(new Generation(new AssistantMessage(value)))); }
    RiskAdviceInput input() {
        return new RiskAdviceInput(List.of(new RiskAdviceInput.Fact("r1","A",CustomerRiskResult.State.NORMAL,
            CustomerRiskResult.Level.HIGH,CustomerRiskResult.History.NEVER_FOLLOWED,20L,14,List.of("NEVER_FOLLOWED"))));
    }
    @Test void validActionsAndProjectionAndNoTools() throws Exception {
        var result=service.generate(input(),20000);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).actions()).containsExactly(RiskAdviceResult.Action.VERIFY_CURRENT_NEEDS);
        var capture=org.mockito.ArgumentCaptor.forClass(Prompt.class); verify(model).call(capture.capture());
        var prompt=capture.getValue();
        String payload=prompt.getInstructions().get(1).getText();
        assertThat(mapper.readTree(payload).at("/customers/0/alias").asText()).isEqualTo("r1");
        assertThat(payload).doesNotContain("customerId","customerName","userId","department","scope","question","address","phone");
        var options=(ToolCallingChatOptions)prompt.getOptions();
        assertThat(options.getToolCallbacks()).isEmpty(); assertThat(options.getInternalToolExecutionEnabled()).isFalse();
        verify(factory).create(Duration.ofSeconds(8));
    }
    @ParameterizedTest @ValueSource(strings={"null","[]","{}","not-json","```json\n{}\n```","{\"items\":null}",
        "{\"items\":[]}","{\"items\":[],\"items\":[]}","{\"items\":[]} {}","{\"items\":[],\"riskLevel\":\"LOW\"}",
        "{\"items\":[{\"alias\":\"r2\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":null,\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":\"VERIFY_CURRENT_NEEDS\",\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[null],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"客户流失概率99%\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"AUTO_CONTACT\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_DATA\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\",\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"FOLLOW_GAP\"]}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"],\"customerId\":\"99\"}]}",
        "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"],\"suggestion\":\"自动联系客户\"}]}",
        "{\"items\":[{\"alias\":\"r1\",\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}"})
    void malformedOrFabricatedOutputNeverEscapes(String value) {
        when(model.call(any(Prompt.class))).thenReturn(text(value));
        assertThatThrownBy(()->service.generate(input(),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
        verify(model).call(any(Prompt.class));
    }
    @Test void inputOrderControlsMappingNotOutputPosition() {
        var one=input().customers().get(0);
        var two=new RiskAdviceInput.Fact("r2",null,CustomerRiskResult.State.DATA_REVIEW,null,
                CustomerRiskResult.History.INVALID_ONLY,null,null,List.of());
        when(model.call(any(Prompt.class))).thenReturn(text("{\"items\":[{\"alias\":\"r2\",\"actions\":[\"VERIFY_DATA\"],\"reasonKeys\":[]},"
            +"{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}]}"));
        var result=service.generate(new RiskAdviceInput(List.of(one,two)),20000);
        assertThat(result.items()).extracting(RiskAdviceResult.Item::alias).containsExactly("r1","r2");
    }
    @Test void duplicateAliasesWithCorrectCountAndExcessActionsAreRejected() {
        var one=input().customers().get(0);
        var two=new RiskAdviceInput.Fact("r2","B",one.state(),one.riskLevel(),one.history(),20L,14,one.reasonKeys());
        String item=VALID.substring(10,VALID.length()-2);
        when(model.call(any(Prompt.class))).thenReturn(text("{\"items\":["+item+","+item+"]}"));
        assertThatThrownBy(()->service.generate(new RiskAdviceInput(List.of(one,two)),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
        doReturn(text(VALID.replace("[\"VERIFY_CURRENT_NEEDS\"]",
                "[\"VERIFY_CURRENT_NEEDS\",\"CONFIRM_CONTACT_PREFERENCE\",\"DISCUSS_NEXT_STEP\",\"VERIFY_DATA\"]")))
            .when(model).call(any(Prompt.class));
        assertThatThrownBy(()->service.generate(input(),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
    }
    @ParameterizedTest @ValueSource(strings={"DUPLICATE","MISSING","EXTRA","REVIEW_ACTION"})
    void coverageAndStateAreChecked(String kind) {
        String item="{\"alias\":\"r1\",\"actions\":[\"VERIFY_CURRENT_NEEDS\"],\"reasonKeys\":[\"NEVER_FOLLOWED\"]}";
        String output=kind.equals("MISSING")?"{\"items\":[]}":"{\"items\":["+item+","+item+"]}";
        var facts=input();
        if(kind.equals("REVIEW_ACTION")) facts=new RiskAdviceInput(List.of(new RiskAdviceInput.Fact("r1","A",
            CustomerRiskResult.State.DATA_REVIEW,null,CustomerRiskResult.History.INVALID_ONLY,null,null,List.of("NEVER_FOLLOWED"))));
        when(model.call(any(Prompt.class))).thenReturn(text(kind.equals("REVIEW_ACTION")?VALID:output));
        var finalFacts=facts;
        assertThatThrownBy(()->service.generate(finalFacts,20000)).isInstanceOf(RiskAdviceUnavailableException.class);
    }
    @Test void oversizedAndEmptyInputAndExpiredBudgetNeverCallModel() {
        limits.setMaxCustomers(1);
        assertThatThrownBy(()->service.generate(new RiskAdviceInput(List.of(input().customers().get(0),input().customers().get(0))),20000))
            .isInstanceOf(RiskAdviceUnavailableException.class);
        assertThatThrownBy(()->service.generate(new RiskAdviceInput(List.of()),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
        assertThatThrownBy(()->service.generate(input(),3500)).isInstanceOf(RiskAdviceUnavailableException.class);
        limits.setMaxInputBytes(1);
        assertThatThrownBy(()->service.generate(input(),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
        verifyNoInteractions(model,factory);
    }
    @Test void outputLengthAndActionLengthBounded() {
        when(model.call(any(Prompt.class))).thenReturn(text("x".repeat(24001)));
        assertThatThrownBy(()->service.generate(input(),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
        doReturn(text(VALID.replace("VERIFY_CURRENT_NEEDS","x".repeat(65)))).when(model).call(any(Prompt.class));
        assertThatThrownBy(()->service.generate(input(),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
    }
    @Test void unavailableClientAndInternalModelExceptionAreControlled() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(()->service.generate(input(),20000)).isInstanceOf(RiskAdviceUnavailableException.class);
        when(provider.getIfAvailable()).thenReturn(factory);
        when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("private raw error"));
        assertThatThrownBy(()->service.generate(input(),20000)).isInstanceOf(RiskAdviceUnavailableException.class)
            .hasMessage("RISK_ADVICE_UNAVAILABLE");
    }
    @Test void concurrencyIsBoundedAndPermitReleasedAfterFailure() throws Exception {
        limits.setMaxConcurrent(1);
        service=new RiskAdviceServiceImpl(provider,mapper,limits,new ClassPathResource("prompts/customer-risk-advice.st"));
        var entered=new CountDownLatch(1); var release=new CountDownLatch(1);
        when(model.call(any(Prompt.class))).thenAnswer(i->{entered.countDown(); if(!release.await(3,TimeUnit.SECONDS))throw new IllegalStateException(); return text(VALID);});
        var executor=Executors.newSingleThreadExecutor();
        try {
            var first=executor.submit(()->service.generate(input(),20000));
            assertThat(entered.await(3,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(()->service.generate(input(),20000)).hasMessage("RISK_ADVICE_BUSY");
            release.countDown(); assertThat(first.get(3,TimeUnit.SECONDS).items()).hasSize(1);
            doThrow(new IllegalStateException()).when(model).call(any(Prompt.class));
            assertThatThrownBy(()->service.generate(input(),20000)).hasMessage("RISK_ADVICE_UNAVAILABLE");
            doReturn(text(VALID)).when(model).call(any(Prompt.class));
            assertThat(service.generate(input(),20000).items()).hasSize(1);
        } finally {release.countDown();executor.shutdownNow();}
    }
    @Test void transportGetsRemainingBudgetAndInvalidConfigurationFailsClosed() {
        service.generate(input(),4000);
        verify(factory).create(Duration.ofMillis(500));
        limits.setTimeoutMillis(-1);
        assertThatThrownBy(()->service.generate(input(),20000)).hasMessage("RISK_ADVICE_NOT_READY");
    }
    @Test void concurrentRequestsKeepTheirOwnFactsAndResults() throws Exception {
        var both=new CountDownLatch(2);
        when(model.call(any(Prompt.class))).thenAnswer(i->{
            Prompt prompt=i.getArgument(0);
            var fact=mapper.readTree(prompt.getInstructions().get(1).getText()).at("/customers/0");
            both.countDown();
            if(!both.await(3,TimeUnit.SECONDS)) throw new IllegalStateException("test barrier");
            return text(fact.get("state").asText().equals("DATA_REVIEW")
                ? "{\"items\":[{\"alias\":\"r1\",\"actions\":[\"VERIFY_DATA\"],\"reasonKeys\":[]}]}" : VALID);
        });
        var review=new RiskAdviceInput(List.of(new RiskAdviceInput.Fact("r1","B",CustomerRiskResult.State.DATA_REVIEW,
                null,CustomerRiskResult.History.INVALID_ONLY,null,null,List.of())));
        var executor=Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(()->service.generate(input(),20000));
            var second=executor.submit(()->service.generate(review,20000));
            assertThat(first.get(4,TimeUnit.SECONDS).items().get(0).actions()).containsExactly(RiskAdviceResult.Action.VERIFY_CURRENT_NEEDS);
            assertThat(second.get(4,TimeUnit.SECONDS).items().get(0).actions()).containsExactly(RiskAdviceResult.Action.VERIFY_DATA);
            verify(model,times(2)).call(any(Prompt.class));
        } finally { executor.shutdownNow(); }
    }
}
