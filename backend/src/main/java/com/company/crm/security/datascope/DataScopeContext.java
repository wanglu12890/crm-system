package com.company.crm.security.datascope;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Resolved scope values passed to Mapper SQL; request parameters cannot alter them. */
@Getter
@RequiredArgsConstructor
public final class DataScopeContext {

    private final DataScopeType scope;
    private final Long userId;
    private final Long deptId;

    public String getScopeCode() {
        return scope.name();
    }
}
