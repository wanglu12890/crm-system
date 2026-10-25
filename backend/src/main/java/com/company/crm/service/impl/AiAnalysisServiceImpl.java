package com.company.crm.service.impl;

import com.company.crm.config.AiRiskChatProperties;
import com.company.crm.exception.*;
import com.company.crm.service.AiAnalysisService;
import com.company.crm.service.AiIntentRouter;
import com.company.crm.service.AiStatisticsAnalysisService;
import com.company.crm.vo.ai.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisServiceImpl implements AiAnalysisService {
    private final AiIntentRouter router;
    private final AiStatisticsAnalysisService statistics;
    private final AiRiskChatProperties riskChat;
    private final com.company.crm.service.AiRiskAnalysisService risks;

    @Override
    public AiAnalysisResponse analyze(Long authenticatedUserId, String question) {
        return analyze(authenticatedUserId, new com.company.crm.dto.ai.AiAnalysisRequest(question));
    }

    @Override
    public AiAnalysisResponse analyze(Long authenticatedUserId, com.company.crm.dto.ai.AiAnalysisRequest request) {
        long startedNanos = System.nanoTime();
        if (authenticatedUserId == null || authenticatedUserId <= 0) {
            log.warn("AI analysis rejected: missing trusted identity");
            throw new AiAuthenticationContextException();
        }
        if (request == null) throw new AiToolArgumentException("分析问题不合法");
        var classification = router.route(request.question());
        if (classification instanceof com.company.crm.dto.ai.internal.AiAnalysisPlan.Statistics plan)
            return statistics.analyze(authenticatedUserId, plan);
        if (classification instanceof com.company.crm.dto.ai.internal.AiAnalysisPlan.Risk plan) {
            if (!riskChat.isEnabled()) {
                log.info("AI risk chat rejected: disabled");
                throw new AiRiskChatUnavailableException(false);
            }
            return risks.analyze(authenticatedUserId, plan, request, startedNanos);
        }
        log.info("AI request rejected: unsupported semantics");
        throw new AiUnsupportedAnalysisException();
    }
}
