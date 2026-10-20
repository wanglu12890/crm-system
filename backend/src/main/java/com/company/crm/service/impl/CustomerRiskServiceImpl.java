package com.company.crm.service.impl;

import com.company.crm.config.CustomerRiskProperties;
import com.company.crm.dto.risk.*;
import com.company.crm.exception.CustomerRiskException;
import com.company.crm.mapper.CustomerRiskMapper;
import com.company.crm.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import static com.company.crm.config.TimeConfig.BUSINESS_ZONE;
import static com.company.crm.exception.CustomerRiskException.Code.*;

@Service
@RequiredArgsConstructor
public class CustomerRiskServiceImpl implements CustomerRiskService {
    private final CustomerRiskAccessPolicy access;
    private final CustomerRiskProperties properties;
    private final CustomerRiskMapper mapper;
    private final CustomerRiskEvaluator evaluator;
    private final Clock businessClock;
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public CustomerRiskPage analyze(Long authenticatedUserId, CustomerRiskQuery query) {
        if (query == null) throw new CustomerRiskException(INVALID_QUERY);
        var scope = access.resolve(authenticatedUserId,query.scope());
        var rules = properties.snapshot();
        var time = ZonedDateTime.now(businessClock).withZoneSameInstant(BUSINESS_ZONE);
        var rows = mapper.selectCandidates(scope,time.toLocalDateTime(),query.levels(),query.customerId(),rules.maxCandidates()+1);
        if (rows == null) throw new CustomerRiskException(INVALID_DATA);
        if (rows.size() > rules.maxCandidates()) throw new CustomerRiskException(CAPACITY_EXCEEDED);
        if (query.operation() == CustomerRiskQuery.Operation.REASON && rows.isEmpty()) throw new CustomerRiskException(NOT_FOUND);
        Set<Long> ids = new HashSet<>();
        List<CustomerRiskResult> results = new ArrayList<>();
        for (var row : rows) {
            if (!ids.add(row.getId())) throw new CustomerRiskException(INVALID_DATA);
            var risk = evaluator.evaluate(row,time,rules);
            if (query.operation() == CustomerRiskQuery.Operation.REASON || query.mode() == CustomerRiskQuery.Mode.ALL_STATES
                    || risk.riskLevel() == CustomerRiskResult.Level.HIGH || risk.riskLevel() == CustomerRiskResult.Level.MEDIUM) results.add(risk);
        }
        results.sort(CustomerRiskEvaluator.ordering());
        long start = (long)(query.page()-1)*query.size();
        int from = (int)Math.min(start,results.size());
        int to = Math.min(from+query.size(),results.size());
        return new CustomerRiskPage(results.subList(from,to),results.size(),query.page(),query.size(),scope.getScopeCode(),rules.version(),time);
    }
}
