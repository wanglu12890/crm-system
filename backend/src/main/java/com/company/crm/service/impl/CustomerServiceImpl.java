package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.mapper.CustomerMapper;
import com.company.crm.security.SecurityUser;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.service.CustomerService;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.CustomerListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final String CUSTOMER_LIST_PERMISSION = "customer:list";

    private final CustomerMapper customerMapper;
    private final DataScopeResolver dataScopeResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<CustomerListVO> listCustomers(CustomerListQuery query) {
        Long currentUserId = currentUserId();
        DataScopeContext dataScope = dataScopeResolver.resolve(currentUserId, CUSTOMER_LIST_PERMISSION);

        Page<CustomerListVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<CustomerListVO> result = customerMapper.selectCustomerPage(page, query, dataScope);
        return new PageResultVO<>(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize(), result.getPages()
        );
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new AuthenticationCredentialsNotFoundException("当前请求缺少有效认证用户");
        }
        return securityUser.getUserId();
    }
}
