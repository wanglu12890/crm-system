package com.company.crm.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.mapper.CustomerMapper;
import com.company.crm.security.SecurityUser;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.security.datascope.DataScopeType;
import com.company.crm.utils.CustomerNumberGenerator;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.CustomerListVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock private CustomerMapper customerMapper;
    @Mock private DataScopeResolver dataScopeResolver;
    @Mock private CustomerNumberGenerator customerNumberGenerator;
    @InjectMocks private CustomerServiceImpl customerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldResolveAllScopeAndReturnMappedPage() {
        authenticate(1L);
        CustomerListQuery query = query(2, 10);
        DataScopeContext scope = new DataScopeContext(DataScopeType.ALL, 1L, null);
        when(dataScopeResolver.resolve(1L, "customer:list")).thenReturn(scope);
        Page<CustomerListVO> result = pageResult(2, 10, 21);
        when(customerMapper.selectCustomerPage(any(), eq(query), eq(scope))).thenReturn(result);

        PageResultVO<CustomerListVO> response = customerService.listCustomers(query);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.total()).isEqualTo(21);
        assertThat(response.pages()).isEqualTo(3);
        assertThat(response.records()).hasSize(1);
    }

    @Test
    void shouldKeepOwnerFilterInsideResolvedDepartmentScope() {
        long managerId = 2107447193143648258L;
        long otherDepartmentOwnerId = 2107683040359124993L;
        authenticate(managerId);
        CustomerListQuery query = query(1, 20);
        query.setOwnerId(otherDepartmentOwnerId);
        DataScopeContext scope = new DataScopeContext(
                DataScopeType.DEPT, managerId, 2206070000000000001L
        );
        when(dataScopeResolver.resolve(managerId, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerPage(any(), eq(query), eq(scope)))
                .thenReturn(new Page<>(1, 20));

        customerService.listCustomers(query);

        verify(customerMapper).selectCustomerPage(any(), eq(query), eq(scope));
        assertThat(query.getOwnerId()).isEqualTo(otherDepartmentOwnerId);
    }

    @Test
    void shouldUseCurrentUserForSelfScope() {
        long staffId = 2107447512812527618L;
        authenticate(staffId);
        CustomerListQuery query = query(1, 20);
        DataScopeContext scope = new DataScopeContext(DataScopeType.SELF, staffId, null);
        when(dataScopeResolver.resolve(staffId, "customer:list")).thenReturn(scope);
        when(customerMapper.selectCustomerPage(any(), eq(query), eq(scope)))
                .thenReturn(new Page<>(1, 20));

        customerService.listCustomers(query);

        verify(dataScopeResolver).resolve(staffId, "customer:list");
        verify(customerMapper).selectCustomerPage(any(), eq(query), eq(scope));
    }

    private void authenticate(long userId) {
        SecurityUser principal = new SecurityUser(
                userId, "user", "password", "User", 1, 0,
                List.of(), List.of("customer:list"), List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private CustomerListQuery query(long page, long size) {
        CustomerListQuery query = new CustomerListQuery();
        query.setPage(page);
        query.setSize(size);
        return query;
    }

    private Page<CustomerListVO> pageResult(long current, long size, long total) {
        Page<CustomerListVO> page = new Page<>(current, size, total);
        page.setRecords(List.of(new CustomerListVO(
                "2206100000000000002", "TEST-CUST-V1-002", "Customer",
                "ENTERPRISE", "B", "IT", "ACTIVE", "1", "Owner", LocalDateTime.now()
        )));
        return page;
    }
}
