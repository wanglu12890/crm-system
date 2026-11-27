package com.company.crm.service;

import com.company.crm.dto.customer.UpdateCustomerDTO;

import com.company.crm.dto.customer.CreateCustomerDTO;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.CustomerCreateVO;
import com.company.crm.vo.customer.CustomerDetailVO;
import com.company.crm.vo.customer.CustomerListVO;

public interface CustomerService {
    void updateCustomer(Long id, UpdateCustomerDTO dto);

    CustomerDetailVO getCustomerDetail(Long id);


    PageResultVO<CustomerListVO> listCustomers(CustomerListQuery query);

    CustomerCreateVO createCustomer(CreateCustomerDTO dto);
}
