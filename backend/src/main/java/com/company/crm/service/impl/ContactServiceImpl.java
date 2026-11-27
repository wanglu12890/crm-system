package com.company.crm.service.impl;

import com.company.crm.mapper.ContactMapper;
import com.company.crm.service.ContactService;
import com.company.crm.service.CustomerService;
import com.company.crm.vo.customer.ContactVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {
    private final CustomerService customerService;
    private final ContactMapper mapper;
    @Override
    @Transactional(readOnly = true)
    public List<ContactVO> listForCustomer(Long id) {
        customerService.getCustomerDetail(id);
        return mapper.selectForCustomer(id);
    }
}
