package com.company.crm.mapper;

import com.company.crm.security.datascope.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CustomerRiskMapperTest {
    static Configuration configuration() throws IOException {
        var configuration = new Configuration();configuration.setMapUnderscoreToCamelCase(true);
        try (var input = CustomerRiskMapperTest.class.getResourceAsStream("/mapper/CustomerRiskMapper.xml")) {
            new XMLMapperBuilder(input,configuration,"risk.xml",configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
    static Map<String,Object> parameters(DataScopeContext scope,Set<String> levels,Long id) {
        Map<String,Object> params = new HashMap<>();params.put("dataScope",scope);params.put("levels",levels);
        params.put("customerId",id);params.put("analysisTime",java.time.LocalDateTime.of(2026,10,18,12,0));params.put("limit",5001);return params;
    }
    static BoundSql sql(Map<String,Object> params) throws IOException {
        return configuration().getMappedStatement("com.company.crm.mapper.CustomerRiskMapper.selectCandidates").getBoundSql(params);
    }
    @Test void queryOnlyProjectsSafeFieldsAndAggregatesAllActors() throws Exception {
        String sql=sql(parameters(new DataScopeContext(DataScopeType.SELF,7L,null),Set.of(),null)).getSql();
        assertThat(sql).contains("c.deleted = 0","c.owner_id IS NOT NULL","'POTENTIAL','ACTIVE'","owner.deleted = 0","owner.status = 1",
                "c.owner_id = ?","f.target_type = 'CUSTOMER'","f.target_id = c.id","f.deleted = 0","LEFT JOIN follow_record",
                "COUNT(f.id)","f.follow_at >= c.created_at","f.follow_at <= ?","LIMIT ?");
        assertThat(sql.toLowerCase()).doesNotContain("f.owner_id","content","phone","remark","next_follow","datediff","now(","curdate(");
        assertThat(sql.indexOf("LIMIT ?")).isLessThan(sql.indexOf("LEFT JOIN"));
    }
    @Test void deptAndReasonAndUnratedAreBoundInsideAuthorization() throws Exception {
        String sql=sql(parameters(new DataScopeContext(DataScopeType.DEPT,7L,20L),Set.of("A","UNRATED"),99L)).getSql();
        assertThat(sql).contains("owner.dept_id = ?","c.id = ?","c.customer_level IS NULL","c.customer_level = ?");
    }
    @ParameterizedTest @ValueSource(strings={"ALL","NULL","DEPT_NULL","SELF_NULL"})
    void malformedScopeAlwaysClosesSql(String scope) throws Exception {
        DataScopeContext context=switch(scope) {
            case "ALL" -> new DataScopeContext(DataScopeType.ALL,7L,null);
            case "DEPT_NULL" -> new DataScopeContext(DataScopeType.DEPT,7L,null);
            case "SELF_NULL" -> new DataScopeContext(DataScopeType.SELF,null,null);
            default -> null;
        };
        assertThat(sql(parameters(context,Set.of(),null)).getSql()).contains("AND 1 = 0");
    }
}
