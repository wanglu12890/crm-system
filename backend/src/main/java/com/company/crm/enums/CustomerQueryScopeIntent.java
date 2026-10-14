package com.company.crm.enums;

/** User-requested scope semantics, never an authorization decision. */
public enum CustomerQueryScopeIntent {
    CURRENT_AUTHORIZED, SELF, DEPT, ALL, UNSUPPORTED
}
