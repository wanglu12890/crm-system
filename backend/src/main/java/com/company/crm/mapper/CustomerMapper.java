package com.company.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.dto.statistics.CustomerStatisticsAggregate;
import com.company.crm.entity.Customer;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.vo.customer.CustomerListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    IPage<CustomerListVO> selectCustomerPage(
            Page<CustomerListVO> page,
            @Param("query") CustomerListQuery query,
            @Param("dataScope") DataScopeContext dataScope
    );

    CustomerStatisticsAggregate selectCustomerStatistics(
            @Param("dataScope") DataScopeContext dataScope,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
