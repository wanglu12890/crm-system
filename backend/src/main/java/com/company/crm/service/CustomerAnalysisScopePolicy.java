package com.company.crm.service;

import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.AiUnsupportedAnalysisException;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.security.datascope.DataScopeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** Resolves an execution scope using server-side authorization and effective roles only. */
@Component
@RequiredArgsConstructor
public class CustomerAnalysisScopePolicy {
    private final DataScopeResolver dataScopeResolver;
    private final SysUserMapper sysUserMapper;

    public DataScopeContext resolve(Long authenticatedUserId, CustomerQueryScopeIntent requestedScope) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
        if (requestedScope == null || requestedScope == CustomerQueryScopeIntent.UNSUPPORTED) {
            throw unsupportedScope();
        }
        DataScopeContext authorizedScope = dataScopeResolver.resolve(authenticatedUserId, "customer:list");
        // Default queries retain the existing widest-scope semantics, including multiple roles.
        if (requestedScope == CustomerQueryScopeIntent.CURRENT_AUTHORIZED) {
            return authorizedScope;
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(authenticatedUserId);
        if (roles == null || roles.size() != 1 || roles.get(0) == null) {
            throw unsupportedScope();
        }
        boolean allowed = switch (roles.get(0)) {
            case "SALES_MANAGER" -> authorizedScope.getScope() == DataScopeType.DEPT
                    && (requestedScope == CustomerQueryScopeIntent.DEPT
                    || requestedScope == CustomerQueryScopeIntent.SELF);
            case "SALES_STAFF" -> authorizedScope.getScope() == DataScopeType.SELF
                    && requestedScope == CustomerQueryScopeIntent.SELF;
            case "SYSTEM_ADMIN", "SUPER_ADMIN" -> authorizedScope.getScope() == DataScopeType.ALL
                    && requestedScope == CustomerQueryScopeIntent.ALL;
            default -> false;
        };
        if (!allowed) {
            throw unsupportedScope();
        }
        return requestedScope == CustomerQueryScopeIntent.SELF
                ? new DataScopeContext(DataScopeType.SELF, authenticatedUserId, null)
                : authorizedScope;
    }

    private AiUnsupportedAnalysisException unsupportedScope() {
        return new AiUnsupportedAnalysisException("请求的客户统计范围不受支持或不符合当前业务角色与授权范围");
    }
}
