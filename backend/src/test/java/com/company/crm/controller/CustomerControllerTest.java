package com.company.crm.controller;

import com.company.crm.config.SecurityConfig;
import com.company.crm.exception.CustomerExceptionHandler;
import com.company.crm.exception.DataScopeConfigurationException;
import com.company.crm.security.CustomUserDetailsService;
import com.company.crm.security.JwtAuthenticationFilter;
import com.company.crm.security.JwtService;
import com.company.crm.security.RestAccessDeniedHandler;
import com.company.crm.security.RestAuthenticationEntryPoint;
import com.company.crm.service.CustomerService;
import com.company.crm.vo.PageResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, CustomerExceptionHandler.class})
class CustomerControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CustomerService customerService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtService jwtService;

    @Test
    void shouldRejectAnonymousCustomerList() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isUnauthorized());
        verify(customerService, never()).listCustomers(any());
    }

    @Test
    void shouldRejectCustomerListWithoutAuthority() throws Exception {
        mockMvc.perform(get("/customers").with(user("user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(customerService, never()).listCustomers(any());
    }

    @Test
    void shouldReturnPagedCustomersWithAuthority() throws Exception {
        when(customerService.listCustomers(any())).thenReturn(new PageResultVO<>(
                List.of(), 0, 2, 10, 0
        ));

        mockMvc.perform(get("/customers")
                        .param("page", "2")
                        .param("size", "10")
                        .param("keyword", "TEST")
                        .with(user("user").authorities(new SimpleGrantedAuthority("customer:list"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.records").isArray());
        verify(customerService).listCustomers(any());
    }

    @Test
    void shouldRejectInvalidPageParameters() throws Exception {
        mockMvc.perform(get("/customers")
                        .param("page", "0")
                        .param("size", "101")
                        .with(user("user").authorities(new SimpleGrantedAuthority("customer:list"))))
                .andExpect(status().isBadRequest());
        verify(customerService, never()).listCustomers(any());
    }

    @Test
    void shouldReturnForbiddenProblemForMissingDepartmentConfiguration() throws Exception {
        when(customerService.listCustomers(any())).thenThrow(
                new DataScopeConfigurationException("当前用户的 DEPT 数据范围未配置部门")
        );

        mockMvc.perform(get("/customers")
                        .with(user("manager").authorities(new SimpleGrantedAuthority("customer:list"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DATA_SCOPE_CONFIGURATION_ERROR"));
    }
}
