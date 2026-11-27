package com.company.crm.mapper;

import com.company.crm.dto.customer.UpdateCustomerDTO;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crm.dto.customer.CustomerListQuery;
import com.company.crm.dto.statistics.CustomerStatisticsAggregate;
import com.company.crm.entity.Customer;
import com.company.crm.security.datascope.DataScopeContext;
import com.company.crm.vo.customer.CustomerDetailVO;
import com.company.crm.vo.customer.CustomerListVO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
    int updateCustomerDetails(@Param("id") Long id,
            @Param("dto") UpdateCustomerDTO dto,
            @Param("operatorId") Long operatorId, @Param("ownerId") Long ownerId,
            @Param("updatedAt") java.time.LocalDateTime updatedAt,
            @Param("dataScope") DataScopeContext dataScope);

    CustomerDetailVO selectCustomerDetail(@Param("id") Long id, @Param("dataScope") DataScopeContext dataScope);


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
