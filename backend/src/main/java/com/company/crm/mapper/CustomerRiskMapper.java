package com.company.crm.mapper;

import com.company.crm.dto.risk.CustomerRiskAggregate;
import com.company.crm.security.datascope.DataScopeContext;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.*;

@Mapper
public interface CustomerRiskMapper {
    List<com.company.crm.dto.risk.CustomerRiskVisibility> selectVisibleCustomers(@Param("dataScope") DataScopeContext scope, @Param("ids") List<Long> ids);
    List<CustomerRiskAggregate> selectCandidates(@Param("dataScope") DataScopeContext scope,
            @Param("analysisTime") LocalDateTime time, @Param("levels") Set<String> levels,
            @Param("customerId") Long customerId, @Param("limit") int limit);
}
