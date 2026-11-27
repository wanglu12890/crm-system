package com.company.crm.mapper;

import com.company.crm.dto.customer.UpdateCustomerDTO;
import com.company.crm.security.datascope.*;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class CustomerUpdateSqlTest {
    private Map<String, Object> params(long id, long owner, int version, DataScopeContext scope) {
        var dto = new UpdateCustomerDTO();
        dto.setCustomerName("rollback-edit-check"); dto.setCustomerType("ENTERPRISE");
        dto.setStatus("ACTIVE"); dto.setVersion(version);
        Map<String, Object> params = new HashMap<>();
        params.put("id", id); params.put("ownerId", owner); params.put("operatorId", owner);
        params.put("dto", dto); params.put("updatedAt", LocalDateTime.now()); params.put("dataScope", scope);
        return params;
    }
    @Test void generatedUpdateKeepsVersionOwnershipAndScopePredicates() throws Exception {
        var config = CustomerRiskMapperTest.configuration();
        for (var type : DataScopeType.values()) {
            var sql = config.getMappedStatement("com.company.crm.mapper.CustomerMapper.updateCustomerDetails")
                .getBoundSql(params(1, 7, 0, new DataScopeContext(type, 7L, 20L))).getSql();
            assertThat(sql).contains("c.deleted = 0", "c.owner_id IS NOT NULL", "c.version = c.version + 1", "c.version = ?", "c.owner_id = ?");
            assertThat(sql.substring(0, sql.indexOf("WHERE"))).doesNotContain("c.owner_id =", "c.customer_no =", "c.created_by =", "c.deleted =");
        }
    }
    private int update(Connection connection, Map<String, Object> params) throws Exception {
        var mapped = CustomerRiskMapperTest.configuration().getMappedStatement("com.company.crm.mapper.CustomerMapper.updateCustomerDetails");
        var bound = mapped.getBoundSql(params);
        try (var statement = connection.prepareStatement(bound.getSql())) {
            new DefaultParameterHandler(mapped, params, bound).setParameters(statement);
            return statement.executeUpdate();
        }
    }
    @Test
    @EnabledIfEnvironmentVariable(named="RUN_CUSTOMER_EDIT_MYSQL", matches="true")
    void actualMysqlUpdateAndStaleVersionAreRolledBack() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/crm_system?serverTimezone=Asia/Shanghai", "root", System.getenv("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                long id, owner; int version;
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT id,owner_id,version FROM customer WHERE deleted=0 AND owner_id IS NOT NULL LIMIT 1 FOR UPDATE")) {
                    assertThat(rows.next()).isTrue(); id=rows.getLong(1); owner=rows.getLong(2); version=rows.getInt(3);
                }
                var params = params(id, owner, version, new DataScopeContext(DataScopeType.SELF, owner, null));
                assertThat(update(connection, params)).isEqualTo(1);
                assertThat(update(connection, params)).isZero();
                try (var statement = connection.prepareStatement("SELECT customer_name,version,updated_by FROM customer WHERE id=?")) {
                    statement.setLong(1,id);
                    try(var rows=statement.executeQuery()) {
                        assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("rollback-edit-check");
                        assertThat(rows.getInt(2)).isEqualTo(version+1); assertThat(rows.getLong(3)).isEqualTo(owner);
                    }
                }
            } finally { connection.rollback(); }
        }
    }
}
