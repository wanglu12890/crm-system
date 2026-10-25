package com.company.crm.service;

import com.company.crm.entity.SysUser;
import com.company.crm.dto.risk.CustomerRiskAccessContext;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.mapper.SysUserMapper;
import com.company.crm.security.datascope.*;
import com.company.crm.exception.CustomerRiskException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import static com.company.crm.exception.CustomerRiskException.Code.ACCESS_DENIED;

@Component
@RequiredArgsConstructor
public class CustomerRiskAccessPolicy {
    public static boolean sameContext(CustomerRiskAccessContext expected, CustomerRiskAccessContext actual) {
        if (expected == null || actual == null || expected.effectiveScope() == null || actual.effectiveScope() == null) return false;
        var a = expected.effectiveScope();
        var b = actual.effectiveScope();
        return java.util.Objects.equals(expected.userId(), actual.userId())
                && java.util.Objects.equals(expected.role(), actual.role())
                && java.util.Objects.equals(expected.departmentId(), actual.departmentId())
                && java.util.Objects.equals(expected.authorizedScope(), actual.authorizedScope())
                && expected.requestedScope() == actual.requestedScope()
                && a.getScope() == b.getScope() && java.util.Objects.equals(a.getUserId(), b.getUserId())
                && java.util.Objects.equals(a.getDeptId(), b.getDeptId());
    }
    private final SysUserMapper users;
    private final DataScopeResolver scopes;
    public DataScopeContext resolve(Long userId, CustomerQueryScopeIntent requested) {
        return resolveContext(userId, requested).effectiveScope();
    }

    public CustomerRiskAccessContext resolveContext(Long userId, CustomerQueryScopeIntent requested) {
        if (userId == null || requested == null) throw new CustomerRiskException(ACCESS_DENIED);
        SysUser user = users.selectById(userId);
        if (user == null || !userId.equals(user.getId()) || !Integer.valueOf(1).equals(user.getStatus())
                || !Integer.valueOf(0).equals(user.getDeleted())) throw new CustomerRiskException(ACCESS_DENIED);
        List<String> roles = users.selectRoleCodesByUserId(userId);
        if (roles == null || roles.size() != 1) throw new CustomerRiskException(ACCESS_DENIED);
        String role = roles.get(0);
        DataScopeType expected;
        if ("SALES_MANAGER".equals(role)) expected = DataScopeType.DEPT;
        else if ("SALES_STAFF".equals(role)) expected = DataScopeType.SELF;
        else throw new CustomerRiskException(ACCESS_DENIED);
        List<String> permissions = users.selectPermissionCodesByUserId(userId);
        if (permissions == null || !permissions.containsAll(List.of("ai:analysis", "customer:list", "follow:list")))
            throw new CustomerRiskException(ACCESS_DENIED);
        DataScopeContext granted;
        try { granted = scopes.resolve(userId, "customer:list"); }
        catch (com.company.crm.exception.DataScopeConfigurationException e) { throw new CustomerRiskException(ACCESS_DENIED); }
        if (granted == null || granted.getScope() != expected || !userId.equals(granted.getUserId())
                || (expected == DataScopeType.DEPT && (user.getDeptId() == null || !user.getDeptId().equals(granted.getDeptId()))))
            throw new CustomerRiskException(ACCESS_DENIED);
        if (requested == CustomerQueryScopeIntent.CURRENT_AUTHORIZED
                || requested == CustomerQueryScopeIntent.SELF
                || (requested == CustomerQueryScopeIntent.DEPT && expected == DataScopeType.DEPT)) {
            var effective = requested == CustomerQueryScopeIntent.SELF
                    ? new DataScopeContext(DataScopeType.SELF, userId, null) : granted;
            return new CustomerRiskAccessContext(userId, role, user.getDeptId(), granted.getScopeCode(), requested, effective);
        }
        throw new CustomerRiskException(CustomerRiskException.Code.SCOPE_UNSUPPORTED);
    }
}
