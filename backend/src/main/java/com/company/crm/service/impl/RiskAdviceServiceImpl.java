package com.company.crm.service.impl;

import com.company.crm.ai.config.RiskAdviceChatClientFactory;
import com.company.crm.config.RiskAdviceProperties;
import com.company.crm.dto.risk.*;
import com.company.crm.dto.risk.RiskAdviceResult.*;
import com.company.crm.exception.RiskAdviceUnavailableException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.company.crm.service.RiskAdviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import static com.company.crm.exception.RiskAdviceUnavailableException.Code.*;

@Service
@Slf4j
public class RiskAdviceServiceImpl implements RiskAdviceService {
    private final ObjectProvider<RiskAdviceChatClientFactory> clients;
    private final ObjectMapper mapper;
    private final RiskAdviceProperties limits;
    private final Semaphore permits;
    private final String policy;
    public RiskAdviceServiceImpl(ObjectProvider<RiskAdviceChatClientFactory> clients, ObjectMapper mapper,
            RiskAdviceProperties limits, @Value("classpath:prompts/customer-risk-advice.st") Resource prompt) throws java.io.IOException {
        this.clients=clients; this.mapper=mapper; this.limits=limits;
        this.permits=new Semaphore(limits.valid()?limits.getMaxConcurrent():1);
        this.policy=prompt.getContentAsString(StandardCharsets.UTF_8);
    }

    @Override public RiskAdviceResult generate(RiskAdviceInput input,long remainingMillis) {
        if (!limits.valid()) throw unavailable(RISK_ADVICE_NOT_READY);
        if (input==null || input.customers().isEmpty() || input.customers().size()>limits.getMaxCustomers())
            throw unavailable(RISK_ADVICE_INPUT_LIMIT);
        validateInput(input);
        String payload;
        try { payload=mapper.writeValueAsString(input); }
        catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw unavailable(RISK_ADVICE_INPUT_LIMIT); }
        // UTF-8 bytes are a conservative input-size/token guard, not a provider tokenizer measurement.
        if (payload.getBytes(StandardCharsets.UTF_8).length+policy.getBytes(StandardCharsets.UTF_8).length>limits.getMaxInputBytes())
            throw unavailable(RISK_ADVICE_INPUT_LIMIT);
        long timeout=Math.min(limits.getTimeoutMillis(),remainingMillis-3500); // Leave room for the final short recheck.
        if (timeout<=0) throw unavailable(RISK_ADVICE_TIMEOUT);
        var factory=clients.getIfAvailable();
        if (factory==null) throw unavailable(RISK_ADVICE_NOT_READY);
        if (!permits.tryAcquire()) throw unavailable(RISK_ADVICE_BUSY);
        long started=System.nanoTime();
        log.info("Risk advice started, customers={}",input.customers().size());
        try {
            var response=factory.create(Duration.ofMillis(timeout)).prompt()
                    .messages(new SystemMessage(policy),new UserMessage(payload))
                    .options(ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build()).call().chatResponse();
            if ((System.nanoTime()-started)/1_000_000>timeout) throw unavailable(RISK_ADVICE_TIMEOUT);
            if (response==null || response.getResults().size()!=1
                    || !response.getResult().getOutput().getToolCalls().isEmpty()
                    || "length".equals(response.getResult().getMetadata().getFinishReason()))
                throw unavailable(RISK_ADVICE_UNAVAILABLE);
            return parse(response.getResult().getOutput().getText(),input);
        } catch (RiskAdviceUnavailableException exception) {
            log.warn("Risk advice degraded, code={}",exception.getCode());
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Risk advice degraded, code=RISK_ADVICE_UNAVAILABLE, failureType={}",exception.getClass().getSimpleName());
            throw unavailable(RISK_ADVICE_UNAVAILABLE);
        } finally {
            permits.release();
            log.info("Risk advice finished, elapsedMs={}",(System.nanoTime()-started)/1_000_000);
        }
    }

    private void validateInput(RiskAdviceInput input) {
        Set<String> aliases=new HashSet<>();
        for (var fact:input.customers()) {
            if (fact.alias()==null || !fact.alias().matches("r([1-9]|[1-4][0-9]|50)") || !aliases.add(fact.alias())
                    || fact.state()==null || fact.history()==null
                    || (fact.customerLevel()!=null && !Set.of("A","B","C","D").contains(fact.customerLevel()))
                    || !Set.of("FOLLOW_GAP","NEVER_FOLLOWED").containsAll(fact.reasonKeys())
                    || new HashSet<>(fact.reasonKeys()).size()!=fact.reasonKeys().size())
                throw unavailable(RISK_ADVICE_INPUT_LIMIT);
        }
    }

    private RiskAdviceResult parse(String content,RiskAdviceInput input) {
        try {
            if (content==null || content.length()>limits.getMaxOutputChars()) throw unavailable(RISK_ADVICE_UNAVAILABLE);
            var root=mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(content);
            exact(root,"items");
            var items=root.get("items");
            if (!items.isArray() || items.size()!=input.customers().size()) throw unavailable(RISK_ADVICE_UNAVAILABLE);
            Map<String,RiskAdviceInput.Fact> facts=new HashMap<>();
            input.customers().forEach(f->facts.put(f.alias(),f));
            Map<String,Item> validated=new HashMap<>();
            for (var node:items) {
                exact(node,"alias","actions","reasonKeys");
                if (!node.get("alias").isTextual()) throw unavailable(RISK_ADVICE_UNAVAILABLE);
                String alias=node.get("alias").textValue();
                var fact=facts.get(alias);
                if (fact==null || validated.containsKey(alias)) throw unavailable(RISK_ADVICE_UNAVAILABLE);
                var actions=strings(node.get("actions"));
                if (actions.isEmpty() || actions.size()>3) throw unavailable(RISK_ADVICE_UNAVAILABLE);
                List<Action> choices=actions.stream().map(Action::valueOf).toList();
                if (fact.state()==CustomerRiskResult.State.DATA_REVIEW
                        ? choices.stream().anyMatch(a->a!=Action.VERIFY_DATA) : choices.contains(Action.VERIFY_DATA))
                    throw unavailable(RISK_ADVICE_UNAVAILABLE);
                var reasons=strings(node.get("reasonKeys"));
                if (!new HashSet<>(reasons).equals(new HashSet<>(fact.reasonKeys()))) throw unavailable(RISK_ADVICE_UNAVAILABLE);
                validated.put(alias,new Item(alias,choices,reasons));
            }
            // Reorder by trusted input, never by model position.
            return new RiskAdviceResult(input.customers().stream().map(f->validated.get(f.alias())).toList());
        } catch (java.io.IOException | IllegalArgumentException exception) { throw unavailable(RISK_ADVICE_UNAVAILABLE); }
    }
    private void exact(JsonNode node,String... names) {
        if (node==null || !node.isObject() || node.size()!=names.length) throw unavailable(RISK_ADVICE_UNAVAILABLE);
        for(String name:names) if (!node.has(name)) throw unavailable(RISK_ADVICE_UNAVAILABLE);
    }
    private List<String> strings(JsonNode node) {
        if (node==null || !node.isArray()) throw unavailable(RISK_ADVICE_UNAVAILABLE);
        List<String> values=new ArrayList<>();
        for(var value:node) {
            if (!value.isTextual() || value.textValue().length()>64 || values.contains(value.textValue()))
                throw unavailable(RISK_ADVICE_UNAVAILABLE);
            values.add(value.textValue());
        }
        return values;
    }
    private RiskAdviceUnavailableException unavailable(RiskAdviceUnavailableException.Code code) {
        return new RiskAdviceUnavailableException(code);
    }
}
