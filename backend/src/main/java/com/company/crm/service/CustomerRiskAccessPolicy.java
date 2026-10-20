package com.company.crm.service;

import com.company.crm.entity.SysUser;
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
    private final SysUserMapper users;
    private final DataScopeResolver scopes;
    public DataScopeContext resolve(Long userId, CustomerQueryScopeIntent requested) {
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
        if (requested == CustomerQueryScopeIntent.CURRENT_AUTHORIZED) return granted;
        if (requested == CustomerQueryScopeIntent.SELF) return new DataScopeContext(DataScopeType.SELF, userId, null);
        if (requested == CustomerQueryScopeIntent.DEPT && expected == DataScopeType.DEPT) return granted;
        throw new CustomerRiskException(ACCESS_DENIED);
    }
}
