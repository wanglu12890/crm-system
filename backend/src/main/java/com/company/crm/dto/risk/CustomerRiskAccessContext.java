package com.company.crm.dto.risk;

import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.security.datascope.DataScopeContext;

/** Server-resolved authorization; never deserialized from HTTP input. */
public record CustomerRiskAccessContext(Long userId, String role, Long departmentId,
        String authorizedScope, CustomerQueryScopeIntent requestedScope, DataScopeContext effectiveScope) {}
