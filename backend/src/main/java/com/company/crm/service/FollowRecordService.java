package com.company.crm.service;

import com.company.crm.vo.PageResultVO;
import com.company.crm.vo.customer.FollowRecordVO;

import com.company.crm.dto.customer.FollowRecordQuery;public interface FollowRecordService {

    PageResultVO<FollowRecordVO> listForCustomer(Long id, FollowRecordQuery query);
}
