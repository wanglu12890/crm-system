package com.company.crm.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements com.company.crm.service.ContactService {
 private final com.company.crm.service.CustomerService customerService;
 private final com.company.crm.mapper.ContactMapper mapper;
 @Transactional(readOnly = true)
 public java.util.List<com.company.crm.vo.customer.ContactVO> listForCustomer(Long id) { customerService.getCustomerDetail(id); return mapper.selectForCustomer(id); }
}
