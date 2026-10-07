package com.company.crm.security.datascope;

import com.company.crm.entity.SysUser;
import com.company.crm.exception.DataScopeConfigurationException;
import com.company.crm.mapper.SysRoleMapper;
import com.company.crm.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** Resolves the strongest effective scope contributed by roles granting a permission. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataScopeResolver {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;

    public DataScopeContext resolve(Long userId, String permissionCode) {
        List<String> scopes = sysRoleMapper.selectGrantedDataScopes(userId, permissionCode);
        if (scopes.contains(DataScopeType.ALL.name())) {
            return new DataScopeContext(DataScopeType.ALL, userId, null);
        }
        if (scopes.contains(DataScopeType.DEPT.name())) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || user.getDeptId() == null) {
                log.warn("Data scope resolution rejected: DEPT user has no department, userId={}, permission={}",
                        userId, permissionCode);
                throw new DataScopeConfigurationException("当前用户的 DEPT 数据范围未配置部门");
            }
            return new DataScopeContext(DataScopeType.DEPT, userId, user.getDeptId());
        }
        if (scopes.contains(DataScopeType.SELF.name())) {
            return new DataScopeContext(DataScopeType.SELF, userId, null);
        }

        log.warn("Data scope resolution rejected: no supported scope, userId={}, permission={}, scopes={}",
                userId, permissionCode, scopes);
        throw new DataScopeConfigurationException("当前用户没有可用的客户数据范围");
    }
}
