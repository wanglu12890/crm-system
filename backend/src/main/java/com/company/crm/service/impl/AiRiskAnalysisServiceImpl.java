package com.company.crm.service.impl;

import com.company.crm.ai.tool.CustomerRiskToolFactory;
import com.company.crm.dto.ai.*;
import com.company.crm.dto.ai.internal.AiAnalysisPlan;
import com.company.crm.dto.risk.*;
import com.company.crm.exception.*;
import com.company.crm.service.*;
import com.company.crm.vo.ai.RiskAnalysisVO;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static com.company.crm.exception.CustomerRiskException.Code.*;

/** Model waits occur outside the core service's short transaction. */
@Service
@Slf4j
public class AiRiskAnalysisServiceImpl implements AiRiskAnalysisService {
    private final ObjectProvider<ChatClient> clients;
    private final ObjectMapper mapper;
    private final CustomerRiskAccessPolicy access;
    private final CustomerRiskToolFactory factory;
    private final CustomerRiskResponseAssembler assembler;
    private final String toolPrompt;
    private final RiskAdviceService advice;
    private final CustomerRiskAdviceAccessVerifier verifier;
    private final com.company.crm.config.RiskAdviceProperties adviceLimits;
    private static final Pattern EXPLICIT_ID = Pattern.compile("(?i)(?:customerId|客户\\s*(?:ID|编号)|ID)\\s*[:：=#]?\\s*([0-9]+)");
    private static final Set<String> INTERACTION = Set.of("customerId", "page", "size", "expectedResultFingerprint");

    public AiRiskAnalysisServiceImpl(@Qualifier("qwenChatClient") ObjectProvider<ChatClient> clients,
            ObjectMapper mapper, CustomerRiskAccessPolicy access, CustomerRiskToolFactory factory,
            CustomerRiskResponseAssembler assembler,
            @Value("classpath:prompts/customer-risk-tool.st") Resource prompt, RiskAdviceService advice,
            CustomerRiskAdviceAccessVerifier verifier, com.company.crm.config.RiskAdviceProperties adviceLimits) throws java.io.IOException {
        this.clients = clients; this.mapper = mapper; this.access = access; this.factory = factory; this.assembler = assembler;
        this.toolPrompt = prompt.getContentAsString(StandardCharsets.UTF_8);
        this.advice = advice; this.verifier = verifier; this.adviceLimits = adviceLimits;
    }

    @Override public RiskAnalysisVO analyze(Long userId, AiAnalysisPlan.Risk candidate, AiAnalysisRequest request) {
        return analyze(userId, candidate, request, System.nanoTime());
    }

    @Override public RiskAnalysisVO analyze(Long userId, AiAnalysisPlan.Risk candidate, AiAnalysisRequest request, long startedNanos) {
        long started = System.nanoTime();
        log.info("AI risk analysis started");
        try {
            if (candidate == null || request == null) throw new CustomerRiskException(QUERY_UNSUPPORTED);
            // Admission first, including all three current database permissions and the single sales role.
            var context = access.resolveContext(userId, candidate.queryScopeIntent());
            var validated = plan(candidate, request);
            var client = clients.getIfAvailable();
            if (client == null) throw new AiAnalysisNotReadyException();
            var tool = factory.create(userId, validated.query(), context);
            org.springframework.ai.chat.model.ChatResponse response;
            try {
                // Raw messages intentionally bypass PromptTemplate parsing of the literal empty JSON object.
                response = client.prompt().messages(new SystemMessage(toolPrompt),
                        new UserMessage("执行已由Java绑定的风险查询，不添加参数。"))
                        .tools(tool).options(ToolCallingChatOptions.builder().internalToolExecutionEnabled(false).build())
                        .call().chatResponse();
            } catch (RuntimeException exception) { throw new AiModelUnavailableException(); }
            if (response == null || response.getResults().size() != 1) throw new AiAnalysisProtocolException();
            var calls = response.getResult().getOutput().getToolCalls();
            if (calls.size() != 1 || !"function".equals(calls.get(0).type())
                    || !"getCustomerRiskAnalysis".equals(calls.get(0).name())) throw new AiAnalysisProtocolException();
            requireEmptyArguments(calls.get(0).arguments());
            var page = tool.getCustomerRiskAnalysis();
            if (!CustomerRiskAccessPolicy.sameContext(context, page.accessContext())
                    || !context.effectiveScope().getScopeCode().equals(page.actualScope())
                    || page.page() != validated.query().page() || page.size() != validated.query().size()
                    || (validated.query().operation() == CustomerRiskQuery.Operation.REASON
                        && (page.items().size() != 1 || !validated.query().customerId().equals(page.items().get(0).customerId()))))
                throw new AiAnalysisProtocolException();
            var base = assembler.assemble(userId, validated, page); // Includes expected fingerprint check before sending facts.
            if (candidate.riskIntent() != AiAnalysisPlan.RiskIntent.SUGGESTIONS) return base;
            return appendAdvice(base, page, startedNanos);
        } catch (RuntimeException exception) {
            // Business/query failures are logged by the shared advice; record model-stage failures here only.
            if (exception instanceof AiAnalysisProtocolException || exception instanceof AiModelUnavailableException
                    || exception instanceof AiAnalysisNotReadyException)
                log.warn("AI risk model stage failed, failureType={}", exception.getClass().getSimpleName());
            throw exception;
        } finally {
            log.info("AI risk analysis finished, elapsedMs={}, totalElapsedMs={}", (System.nanoTime() - started) / 1_000_000,
                    (System.nanoTime() - startedNanos) / 1_000_000);
        }
    }

    private RiskAnalysisVO appendAdvice(RiskAnalysisVO base, CustomerRiskPage page, long startedNanos) {
        var ids = page.items().stream().map(CustomerRiskResult::customerId).toList();
        var visible = verifier.verify(page.accessContext(), ids);
        if (ids.isEmpty()) return base;
        RiskAnalysisVO.Advice generated;
        try {
            long remaining = adviceLimits.getRequestBudgetMillis() - (System.nanoTime() - startedNanos) / 1_000_000;
            var result = advice.generate(RiskAdviceInput.from(page), remaining);
            Map<String,RiskAdviceResult.Item> byAlias = new HashMap<>();
            for (var item : result.items()) if (byAlias.putIfAbsent(item.alias(), item) != null)
                throw new RiskAdviceUnavailableException(RiskAdviceUnavailableException.Code.RISK_ADVICE_UNAVAILABLE);
            if (byAlias.size() != ids.size())
                throw new RiskAdviceUnavailableException(RiskAdviceUnavailableException.Code.RISK_ADVICE_UNAVAILABLE);
            List<Object> rendered = new ArrayList<>();
            for (int i=0;i<ids.size();i++) {
                var item = byAlias.remove("r" + (i+1));
                if (item == null) throw new RiskAdviceUnavailableException(RiskAdviceUnavailableException.Code.RISK_ADVICE_UNAVAILABLE);
                rendered.add(new RiskAdviceResult.RenderedItem(ids.get(i).toString(),
                        item.actions().stream().map(Enum::name).toList(), item.reasonKeys(),
                        item.actions().stream().map(RiskAdviceResult.Action::text).collect(java.util.stream.Collectors.joining(" "))));
            }
            generated = new RiskAnalysisVO.Advice("AVAILABLE", "QWEN_ACTIONS_JAVA_TEMPLATE", null,
                    "辅助建议仅供参考，请人工核实后决定是否执行。", List.copyOf(rendered));
            log.info("Risk advice ready, customers={}", rendered.size());
        } catch (RiskAdviceUnavailableException exception) {
            generated = new RiskAnalysisVO.Advice("UNAVAILABLE", null, exception.getCode().name(),
                    "建议暂不可用，已保留关注分析结果。", List.of());
            log.info("Risk advice unavailable, code={}", exception.getCode());
        }
        // Always recheck, including model failures; access failures must not be swallowed as PARTIAL.
        verifier.verify(page.accessContext(), ids, visible);
        return new RiskAnalysisVO("AVAILABLE".equals(generated.status()) ? "SUCCESS" : "PARTIAL", base.intent(),
                base.answer(), base.analysisContext(), base.normalizedQuery(), base.page(), base.records(),
                base.resultFingerprint(), generated);
    }

    private CustomerRiskRequest plan(AiAnalysisPlan.Risk candidate, AiAnalysisRequest request) {
        var fields = request.interaction();
        if (!INTERACTION.containsAll(fields.keySet())) throw new CustomerRiskException(QUERY_UNSUPPORTED);
        boolean reason = candidate.riskIntent() == AiAnalysisPlan.RiskIntent.REASON;
        Long id = null;
        if (reason) {
            if (!fields.containsKey("customerId")) throw new CustomerRiskException(CUSTOMER_SELECTION_REQUIRED);
            var value = fields.get("customerId");
            if (!value.isTextual() || !value.textValue().matches("[1-9][0-9]{0,18}"))
                throw new CustomerRiskException(QUERY_UNSUPPORTED);
            try { id = Long.valueOf(value.textValue()); }
            catch (NumberFormatException exception) { throw new CustomerRiskException(QUERY_UNSUPPORTED); }
            var matches = EXPLICIT_ID.matcher(request.question());
            while (matches.find()) if (!matches.group(1).equals(value.textValue()))
                throw new CustomerRiskException(CUSTOMER_SELECTION_REQUIRED);
            if (fields.size() != 1 || candidate.queryMode() != CustomerRiskQuery.Mode.PRIORITY || !candidate.customerLevels().isEmpty())
                throw new CustomerRiskException(QUERY_UNSUPPORTED);
        } else if (fields.containsKey("customerId")) throw new CustomerRiskException(QUERY_UNSUPPORTED);
        String fingerprint = null;
        if (fields.containsKey("expectedResultFingerprint")) {
            var node = fields.get("expectedResultFingerprint");
            if (candidate.riskIntent() != AiAnalysisPlan.RiskIntent.SUGGESTIONS || !node.isTextual()
                    || node.textValue().isBlank() || node.textValue().length() > 128)
                throw new CustomerRiskException(QUERY_UNSUPPORTED);
            fingerprint = node.textValue();
        }
        try { return new CustomerRiskRequest(CustomerRiskRequest.Action.valueOf(candidate.riskIntent().name()),
                new CustomerRiskQuery(reason ? CustomerRiskQuery.Operation.REASON : CustomerRiskQuery.Operation.LIST,
                        candidate.queryMode(), candidate.queryScopeIntent(), candidate.customerLevels(), id,
                        integer(fields, "page", 1), integer(fields, "size", 20)), fingerprint);
        } catch (CustomerRiskException exception) { throw new CustomerRiskException(QUERY_UNSUPPORTED); }
    }

    private int integer(Map<String, JsonNode> fields, String key, int fallback) {
        if (!fields.containsKey(key)) return fallback;
        var node = fields.get(key);
        if (!node.isIntegralNumber() || !node.canConvertToInt()) throw new CustomerRiskException(QUERY_UNSUPPORTED);
        return node.intValue();
    }

    private void requireEmptyArguments(String arguments) {
        try {
            if (arguments == null) throw new AiAnalysisProtocolException();
            var value = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(arguments);
            if (value == null || !value.isObject() || !value.isEmpty()) throw new AiAnalysisProtocolException();
        } catch (java.io.IOException exception) { throw new AiAnalysisProtocolException(); }
    }
}
