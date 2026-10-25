package com.company.crm.service;

import com.company.crm.dto.ai.CustomerRiskRequest;
import com.company.crm.dto.risk.*;
import com.company.crm.exception.CustomerRiskException;
import com.company.crm.vo.ai.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import static com.company.crm.config.TimeConfig.BUSINESS_ZONE;
import static com.company.crm.exception.CustomerRiskException.Code.*;

/** Maps an already queried trusted snapshot; never reads the database. */
@Service
@RequiredArgsConstructor
public class CustomerRiskResponseAssembler {
    private final ObjectMapper objectMapper;

    public RiskAnalysisVO assemble(Long userId, CustomerRiskRequest request, CustomerRiskPage page) {
        if (userId == null || userId <= 0) throw new CustomerRiskException(AUTHENTICATION_REQUIRED);
        if (request == null) throw new CustomerRiskException(INVALID_QUERY);
        var access = page.accessContext();
        if (access == null || !userId.equals(access.userId())) throw new CustomerRiskException(INVALID_DATA);
        var query = request.query();
        boolean reason = query.operation() == CustomerRiskQuery.Operation.REASON;
        var levels = query.levels().stream().sorted().toList();
        RiskAnalysisVO.NormalizedQuery normalized = reason
                ? new RiskAnalysisVO.ReasonQuery(query.scope().name(), query.customerId().toString())
                : new RiskAnalysisVO.ListQuery(query.scope().name(), query.mode().name(),
                        levels.isEmpty() ? null : levels, query.page(), query.size());
        var records = page.items().stream().map(this::record).toList();
        String fingerprint = fingerprint(userId, page, normalized, records);
        if (request.expectedResultFingerprint() != null && !request.expectedResultFingerprint().equals(fingerprint))
            throw new CustomerRiskException(RESULT_STALE);
        boolean suggestions = request.action() == CustomerRiskRequest.Action.SUGGESTIONS;
        boolean unavailable = suggestions && !records.isEmpty();
        var advice = new RiskAnalysisVO.Advice(!suggestions ? "NOT_REQUESTED" : unavailable ? "UNAVAILABLE" : "NOT_APPLICABLE",
                null, unavailable ? "RISK_ADVICE_NOT_READY" : null,
                unavailable ? "建议暂不可用，已保留关注分析结果。" : null, List.of());
        var context = new RiskAnalysisContextVO(access.authorizedScope(), access.requestedScope().name(), page.actualScope(),
                page.analysisTime().toOffsetDateTime(), BUSINESS_ZONE.getId(), page.ruleVersion(), "LIVE_REQUEST_SNAPSHOT");
        String scopeLabel = "SELF".equals(page.actualScope()) ? "本人范围内" : "本部门范围内";
        String answer = reason ? scopeLabel + "，" + summary(page.items().get(0)) + "。"
                : scopeLabel + "匹配到 " + page.total() + " 个" + (query.mode() == CustomerRiskQuery.Mode.PRIORITY
                ? "高/中关注客户" : "适用客户关注状态") + "，本页返回 " + records.size() + " 个。";
        return new RiskAnalysisVO(unavailable ? "PARTIAL" : "SUCCESS", "RISK_" + request.action().name(), answer, context,
                normalized, reason ? null : new RiskAnalysisVO.Page(page.page(), page.size(), records.size(), page.total(),
                (page.total() + page.size() - 1) / page.size()), records, fingerprint, advice);
    }

    private RiskRecordVO record(CustomerRiskResult result) {
        var reasons = result.reasons().stream().map(code -> new RiskRecordVO.Reason(code,
                summary(result), new RiskRecordVO.Evidence(offset(result.anchorTime()), result.elapsedDays(),
                result.thresholdDays(), result.history() == CustomerRiskResult.History.NEVER_FOLLOWED ? "NEVER"
                : "A".equals(result.customerLevel()) ? "A" : "OTHER"))).toList();
        return new RiskRecordVO(result.customerId().toString(), result.customerNo(), result.customerName(), result.customerLevel(),
                result.customerStatus(), result.state().name(), result.riskLevel() == null ? null : result.riskLevel().name(),
                result.history().name(), offset(result.lastValidFollowAt()), result.anchorType(), result.elapsedDays(),
                result.state() == CustomerRiskResult.State.OBSERVING ? null : result.thresholdDays(), result.excessDays(), reasons,
                new RiskRecordVO.DataQuality(result.qualityHints().isEmpty() ? "CLEAN" : "HAS_ANOMALIES", result.qualityHints()));
    }

    private OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }

    private String summary(CustomerRiskResult result) {
        if (result.state() == CustomerRiskResult.State.DATA_REVIEW) return "客户数据待核验，暂不评定关注等级";
        if (result.state() == CustomerRiskResult.State.OBSERVING) return "客户从未跟进，建档 " + result.elapsedDays() + " 天，处于观察期";
        String anchor = result.history() == CustomerRiskResult.History.NEVER_FOLLOWED
                ? "客户从未跟进，建档 " : "距最近有效跟进 ";
        String level = switch (result.riskLevel()) { case HIGH -> "高关注"; case MEDIUM -> "中关注"; case LOW -> "低关注"; };
        return anchor + result.elapsedDays() + " 天，当前为" + level
                + (result.riskLevel() == CustomerRiskResult.Level.LOW ? "" : "，已达到 " + result.thresholdDays() + " 天阈值");
    }

    private String fingerprint(Long userId, CustomerRiskPage page, RiskAnalysisVO.NormalizedQuery query, List<RiskRecordVO> records) {
        try {
            var access = page.accessContext();
            // Fixed ordered tuple, canonical sorted levels and records; deliberately excludes changing analysis time.
            var tuple = Arrays.asList("risk-fingerprint-v1", userId, access.role(), access.departmentId(),
                    access.authorizedScope(), page.actualScope(), query, page.ruleVersion(), page.total(), records);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsString(tuple).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new CustomerRiskException(INVALID_DATA);
        }
    }
}
