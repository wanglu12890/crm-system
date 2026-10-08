package com.company.crm.service.impl;

import com.company.crm.dto.customer.CreateCustomerDTO;
import com.company.crm.entity.Customer;
import com.company.crm.exception.CustomerCreationException;
import com.company.crm.exception.DuplicateCustomerNumberException;
import com.company.crm.exception.ForbiddenCustomerCreationException;
import com.company.crm.mapper.CustomerMapper;
import com.company.crm.security.SecurityUser;
import com.company.crm.security.datascope.DataScopeResolver;
import com.company.crm.utils.CustomerNumberGenerator;
import com.company.crm.vo.customer.CustomerCreateVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCreationServiceTest {

    private static final long OPERATOR_ID = 2107447512812527618L;

    @Mock private CustomerMapper customerMapper;
    @Mock private DataScopeResolver dataScopeResolver;
    @Mock private CustomerNumberGenerator customerNumberGenerator;
    @InjectMocks private CustomerServiceImpl customerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({
            "SUPER_ADMIN, false",
            "SYSTEM_ADMIN, false",
            "SALES_MANAGER, true",
            "SALES_STAFF, true"
    })
    void shouldAssignOwnerAccordingToEffectiveRole(String roleCode, boolean privateCustomer) {
        authenticate(List.of(roleCode));
        stubSuccessfulInsert(2207000000000000001L);

        CustomerCreateVO result = customerService.createCustomer(validDto());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(captor.capture());
        Customer saved = captor.getValue();
        assertThat(saved.getOwnerId()).isEqualTo(privateCustomer ? OPERATOR_ID : null);
        assertThat(saved.getCustomerNo()).isEqualTo("KH2207000000000000001");
        assertThat(saved.getCustomerType()).isEqualTo("ENTERPRISE");
        assertThat(saved.getStatus()).isEqualTo("POTENTIAL");
        assertThat(saved.getCreatedBy()).isEqualTo(OPERATOR_ID);
        assertThat(saved.getUpdatedBy()).isEqualTo(OPERATOR_ID);
        assertThat(result.id()).isEqualTo("2207000000000000001");
    }

    @Test
    void shouldRejectAmbiguousSystemAndSalesRolesWithoutWriting() {
        authenticate(List.of("SYSTEM_ADMIN", "SALES_MANAGER"));

        assertThatThrownBy(() -> customerService.createCustomer(validDto()))
                .isInstanceOf(ForbiddenCustomerCreationException.class);

        verify(customerNumberGenerator, never()).nextId();
        verify(customerMapper, never()).insert(any(Customer.class));
    }

    @Test
    void shouldRejectUnsupportedRoleWithoutWriting() {
        authenticate(List.of("CUSTOM_ROLE"));

        assertThatThrownBy(() -> customerService.createCustomer(validDto()))
                .isInstanceOf(ForbiddenCustomerCreationException.class);
        verify(customerMapper, never()).insert(any(Customer.class));
    }

    @Test
    void shouldAllowDuplicateCustomerNamesWithDifferentGeneratedNumbers() {
        authenticate(List.of("SALES_STAFF"));
        when(customerNumberGenerator.nextId()).thenReturn(101L, 102L);
        when(customerNumberGenerator.customerNo(101L)).thenReturn("KH101");
        when(customerNumberGenerator.customerNo(102L)).thenReturn("KH102");
        when(customerMapper.insert(any(Customer.class))).thenReturn(1);

        customerService.createCustomer(validDto());
        customerService.createCustomer(validDto());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(Customer::getCustomerName)
                .containsExactly("测试客户", "测试客户");
        assertThat(captor.getAllValues()).extracting(Customer::getCustomerNo)
                .containsExactly("KH101", "KH102");
    }

    @Test
    void shouldTranslateUniqueConstraintConflict() {
        authenticate(List.of("SUPER_ADMIN"));
        when(customerNumberGenerator.nextId()).thenReturn(101L);
        when(customerNumberGenerator.customerNo(101L)).thenReturn("KH101");
        when(customerMapper.insert(any(Customer.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> customerService.createCustomer(validDto()))
                .isInstanceOf(DuplicateCustomerNumberException.class);
    }

    @Test
    void shouldNotReturnSuccessWhenDatabaseSaveFails() {
        authenticate(List.of("SALES_MANAGER"));
        when(customerNumberGenerator.nextId()).thenReturn(101L);
        when(customerNumberGenerator.customerNo(101L)).thenReturn("KH101");
        when(customerMapper.insert(any(Customer.class)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThatThrownBy(() -> customerService.createCustomer(validDto()))
                .isInstanceOf(CustomerCreationException.class)
                .hasMessage("客户保存失败，请稍后重试");
    }

    @Test
    void shouldNormalizeOptionalValuesAndKeepExplicitEnums() {
        authenticate(List.of("SALES_STAFF"));
        stubSuccessfulInsert(101L);
        CreateCustomerDTO dto = validDto();
        dto.setCustomerType("INDIVIDUAL");
        dto.setCustomerLevel("A");
        dto.setStatus("ACTIVE");
        dto.setIndustry("  信息技术  ");
        dto.setPhone("   ");

        customerService.createCustomer(dto);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).insert(captor.capture());
        assertThat(captor.getValue().getCustomerType()).isEqualTo("INDIVIDUAL");
        assertThat(captor.getValue().getCustomerLevel()).isEqualTo("A");
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getIndustry()).isEqualTo("信息技术");
        assertThat(captor.getValue().getPhone()).isNull();
    }

    private void stubSuccessfulInsert(long customerId) {
        when(customerNumberGenerator.nextId()).thenReturn(customerId);
        when(customerNumberGenerator.customerNo(customerId)).thenReturn("KH" + customerId);
        when(customerMapper.insert(any(Customer.class))).thenReturn(1);
    }

    private CreateCustomerDTO validDto() {
        CreateCustomerDTO dto = new CreateCustomerDTO();
        dto.setCustomerName("  测试客户  ");
        return dto;
    }

    private void authenticate(List<String> roles) {
        SecurityUser principal = new SecurityUser(
                OPERATOR_ID, "operator", "password", "Operator", 1, 0,
                roles, List.of("customer:create"), List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
