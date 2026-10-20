package com.company.crm.dto.risk;

import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.CustomerRiskException;
import java.util.*;

public record CustomerRiskQuery(Operation operation, Mode mode, CustomerQueryScopeIntent scope,
        Set<String> levels, Long customerId, int page, int size) {
    public enum Operation { LIST, REASON }
    public enum Mode { PRIORITY, ALL_STATES }
    public CustomerRiskQuery {
        if (operation == null || mode == null || scope == null || page < 1 || size < 1 || size > 50
                || (operation == Operation.REASON && (customerId == null || customerId <= 0))
                || (operation == Operation.LIST && customerId != null))
            throw new CustomerRiskException(CustomerRiskException.Code.INVALID_QUERY);
        if (levels != null && levels.stream().anyMatch(Objects::isNull)) throw new CustomerRiskException(CustomerRiskException.Code.INVALID_QUERY);
        levels = levels == null ? Set.of() : Set.copyOf(levels);
        if (operation == Operation.REASON && (!levels.isEmpty() || page != 1 || size != 20 || mode != Mode.PRIORITY))
            throw new CustomerRiskException(CustomerRiskException.Code.INVALID_QUERY);
        if (!Set.of("A","B","C","D","UNRATED").containsAll(levels)) throw new CustomerRiskException(CustomerRiskException.Code.INVALID_QUERY);
    }
    public static CustomerRiskQuery priority() {
        return new CustomerRiskQuery(Operation.LIST,Mode.PRIORITY,CustomerQueryScopeIntent.CURRENT_AUTHORIZED,Set.of(),null,1,20);
    }
}
