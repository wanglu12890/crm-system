package com.company.crm.service;

import com.company.crm.dto.risk.CustomerRiskAccessContext;
import com.company.crm.dto.risk.CustomerRiskVisibility;
import com.company.crm.exception.CustomerRiskException;
import com.company.crm.mapper.CustomerRiskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static com.company.crm.exception.CustomerRiskException.Code.*;

/** Each invocation starts a fresh short snapshot; never spans a model wait. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRiskAdviceAccessVerifier {
    private final CustomerRiskAccessPolicy policy;
    private final CustomerRiskMapper mapper;

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ, propagation=Propagation.REQUIRES_NEW)
    public List<CustomerRiskVisibility> verify(CustomerRiskAccessContext expected, List<Long> ids) {
        return execute(expected, ids, null);
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ, propagation=Propagation.REQUIRES_NEW)
    public void verify(CustomerRiskAccessContext expected, List<Long> ids, List<CustomerRiskVisibility> previous) {
        execute(expected, ids, previous);
    }

    private List<CustomerRiskVisibility> execute(CustomerRiskAccessContext expected, List<Long> ids, List<CustomerRiskVisibility> previous) {
        try {
            if (expected==null || ids==null || ids.size()>50 || new HashSet<>(ids).size()!=ids.size())
                throw new CustomerRiskException(ACCESS_CONTEXT_CHANGED);
            var current = policy.resolveContext(expected.userId(),expected.requestedScope());
            if (!CustomerRiskAccessPolicy.sameContext(expected,current)) throw new CustomerRiskException(ACCESS_CONTEXT_CHANGED);
            if (ids.isEmpty()) return List.of();
            var visible = mapper.selectVisibleCustomers(current.effectiveScope(),ids);
            if (visible==null || visible.size()!=ids.size()
                    || !new HashSet<>(visible.stream().map(CustomerRiskVisibility::customerId).toList()).equals(new HashSet<>(ids))
                    || (previous!=null && !new HashSet<>(previous).equals(new HashSet<>(visible))))
                throw new CustomerRiskException(RESULT_STALE);
            return List.copyOf(visible);
        } catch (CustomerRiskException exception) {
            log.warn("Risk advice access recheck failed, code={}", exception.getCode());
            if (exception.getCode()==RESULT_STALE) throw exception;
            throw new CustomerRiskException(ACCESS_CONTEXT_CHANGED);
        }
    }
}
