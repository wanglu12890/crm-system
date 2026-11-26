package com.company.crm.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class FollowRecordServiceImpl implements com.company.crm.service.FollowRecordService {
 private final com.company.crm.service.CustomerService customerService;
 private final com.company.crm.mapper.FollowRecordMapper mapper;
 @Transactional(readOnly = true)
 public com.company.crm.vo.PageResultVO<com.company.crm.vo.customer.FollowRecordVO> listForCustomer(Long id, com.company.crm.dto.customer.FollowRecordQuery query) { customerService.getCustomerDetail(id); var result = mapper.selectForCustomer(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getPage(), query.getSize()), id); return new com.company.crm.vo.PageResultVO<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize(), result.getPages()); }
}
