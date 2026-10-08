package com.company.crm.service;

import com.company.crm.dto.customer.CreateCustomerDTO;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.CustomerCreateVO;
import com.company.crm.vo.customer.CustomerListVO;

public interface CustomerService {

    PageResultVO<CustomerListVO> listCustomers(CustomerListQuery query);

    CustomerCreateVO createCustomer(CreateCustomerDTO dto);
}
