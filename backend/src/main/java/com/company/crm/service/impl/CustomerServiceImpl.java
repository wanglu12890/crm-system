package com.company.crm.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crm.dto.customer.CreateCustomerDTO;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.entity.Customer;
import com.company.crm.exception.CustomerCreationException;
import com.company.crm.exception.DuplicateCustomerNumberException;
import com.company.crm.exception.ForbiddenCustomerCreationException;
import com.company.crm.mapper.CustomerMapper;
import com.company.crm.security.SecurityUser;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.service.CustomerService;
import com.company.crm.utils.CustomerNumberGenerator;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.CustomerCreateVO;
import com.company.crm.vo.customer.CustomerListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private static final String CUSTOMER_LIST_PERMISSION = "customer:list";
    private static final Set<String> SYSTEM_ROLE_CODES = Set.of("SUPER_ADMIN", "SYSTEM_ADMIN");
    private static final Set<String> SALES_ROLE_CODES = Set.of("SALES_MANAGER", "SALES_STAFF");
    private static final String DEFAULT_CUSTOMER_TYPE = "ENTERPRISE";
    private static final String DEFAULT_CUSTOMER_STATUS = "POTENTIAL";

    private final CustomerMapper customerMapper;
    private final DataScopeResolver dataScopeResolver;
    private final CustomerNumberGenerator customerNumberGenerator;

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<CustomerListVO> listCustomers(CustomerListQuery query) {
        Long currentUserId = currentSecurityUser().getUserId();
        DataScopeContext dataScope = dataScopeResolver.resolve(currentUserId, CUSTOMER_LIST_PERMISSION);

        Page<CustomerListVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<CustomerListVO> result = customerMapper.selectCustomerPage(page, query, dataScope);
        return new PageResultVO<>(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize(), result.getPages()
        );
    }

    @Override
    @Transactional
    public CustomerCreateVO createCustomer(CreateCustomerDTO dto) {
        SecurityUser operator = currentSecurityUser();
        Long operatorId = operator.getUserId();
        Long ownerId = resolveCreatedCustomerOwner(operator);
        Long customerId = customerNumberGenerator.nextId();
        String customerNo = customerNumberGenerator.customerNo(customerId);
        LocalDateTime now = LocalDateTime.now();

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCustomerNo(customerNo);
        customer.setCustomerName(dto.getCustomerName().trim());
        customer.setCustomerType(valueOrDefault(dto.getCustomerType(), DEFAULT_CUSTOMER_TYPE));
        customer.setCustomerLevel(nullableTrimmed(dto.getCustomerLevel()));
        customer.setIndustry(nullableTrimmed(dto.getIndustry()));
        customer.setSource(nullableTrimmed(dto.getSource()));
        customer.setPhone(nullableTrimmed(dto.getPhone()));
        customer.setEmail(nullableTrimmed(dto.getEmail()));
        customer.setProvince(nullableTrimmed(dto.getProvince()));
        customer.setCity(nullableTrimmed(dto.getCity()));
        customer.setAddress(nullableTrimmed(dto.getAddress()));
        customer.setOwnerId(ownerId);
        customer.setStatus(valueOrDefault(dto.getStatus(), DEFAULT_CUSTOMER_STATUS));
        customer.setRemark(nullableTrimmed(dto.getRemark()));
        customer.setCreatedBy(operatorId);
        customer.setUpdatedBy(operatorId);
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customer.setDeleted(0);
        customer.setVersion(0);

        try {
            if (customerMapper.insert(customer) != 1) {
                throw new CustomerCreationException("客户保存失败");
            }
        } catch (DuplicateKeyException exception) {
            log.warn("Create customer rejected by unique constraint, operatorUserId={}, customerNo={}",
                    operatorId, customerNo);
            throw new DuplicateCustomerNumberException();
        } catch (DataAccessException exception) {
            log.error("Create customer database failure, operatorUserId={}, customerNo={}, failureType={}",
                    operatorId, customerNo, exception.getClass().getSimpleName());
            throw new CustomerCreationException("客户保存失败，请稍后重试", exception);
        }

        log.info("Create customer success, operatorUserId={}, customerId={}, customerNo={}, ownership={}",
                operatorId, customerId, customerNo, ownerId == null ? "PUBLIC_POOL" : "PRIVATE");
        return new CustomerCreateVO(String.valueOf(customerId), customerNo);
    }

    private Long resolveCreatedCustomerOwner(SecurityUser operator) {
        Set<String> roles = new HashSet<>(operator.getRoles());
        boolean systemOnly = !roles.isEmpty() && SYSTEM_ROLE_CODES.containsAll(roles);
        boolean salesOnly = !roles.isEmpty() && SALES_ROLE_CODES.containsAll(roles);
        if (systemOnly) {
            return null;
        }
        if (salesOnly) {
            return operator.getUserId();
        }

        log.warn("Create customer rejected: ambiguous or unsupported role combination, operatorUserId={}, roles={}",
                operator.getUserId(), roles);
        throw new ForbiddenCustomerCreationException("当前用户角色组合无法确定客户归属");
    }

    private String valueOrDefault(String value, String defaultValue) {
        String normalized = nullableTrimmed(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String nullableTrimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SecurityUser currentSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new AuthenticationCredentialsNotFoundException("当前请求缺少有效认证用户");
        }
        return securityUser;
    }
}
