package com.company.crm.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crm.dto.customer.FollowRecordQuery;
import com.company.crm.mapper.FollowRecordMapper;
import com.company.crm.service.CustomerService;
import com.company.crm.service.FollowRecordService;
import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.FollowRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowRecordServiceImpl implements FollowRecordService {
    private final CustomerService customerService;
    private final FollowRecordMapper mapper;
    @Override
    @Transactional(readOnly = true)
    public PageResultVO<FollowRecordVO> listForCustomer(Long id, FollowRecordQuery query) {
        customerService.getCustomerDetail(id);
        var result = mapper.selectForCustomer(new Page<>(query.getPage(), query.getSize()), id);
        return new PageResultVO<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }
}
