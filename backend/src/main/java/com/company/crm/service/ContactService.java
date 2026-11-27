package com.company.crm.service;

import java.util.List;

import com.company.crm.vo.customer.ContactVO;public interface ContactService {

    List<ContactVO> listForCustomer(Long id);
}
