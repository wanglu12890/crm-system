package com.company.crm.mapper;

import com.company.crm.security.datascope.*;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Opt-in real MySQL SELECT-only tests. No schema, fixtures, login or initialization writes. */
@EnabledIfEnvironmentVariable(named="RUN_CUSTOMER_RISK_MYSQL",matches="true")
class CustomerRiskMapperMySqlTest {
    Connection connection;
    @BeforeEach void connect() throws Exception {
        connection=DriverManager.getConnection(System.getenv("CRM_RISK_TEST_JDBC_URL"),System.getenv("CRM_RISK_TEST_DB_USER"),System.getenv("CRM_RISK_TEST_DB_PASSWORD"));
        connection.setReadOnly(true);connection.setAutoCommit(false);
        try(var statement=connection.createStatement()) {statement.execute("START TRANSACTION READ ONLY");}
    }
    @AfterEach void close() throws Exception { if(connection!=null && !connection.isClosed()) {connection.rollback();connection.close();} }
    List<Map<String,Object>> query(Map<String,Object> params,String fixtures) throws Exception {
        var configuration=CustomerRiskMapperTest.configuration();
        var mapped=configuration.getMappedStatement("com.company.crm.mapper.CustomerRiskMapper.selectCandidates");
        var bound=mapped.getBoundSql(params);String sql=bound.getSql();
        if(fixtures!=null) sql=sql.replaceFirst("WITH eligible AS",fixtures+", eligible AS")
                .replace("FROM customer c","FROM (SELECT fc.*,CAST(fc.id AS CHAR) AS customer_no FROM fixture_customer fc) c").replace("JOIN sys_user owner","JOIN fixture_user owner")
                .replace("JOIN follow_record f","JOIN fixture_follow f");
        List<Map<String,Object>> rows=new ArrayList<>();
        try(var statement=connection.prepareStatement(sql)) {
            new DefaultParameterHandler(mapped,params,bound).setParameters(statement);
            try(var result=statement.executeQuery()) {while(result.next()) {
                Map<String,Object> row=new HashMap<>();for(int i=1;i<=result.getMetaData().getColumnCount();i++) row.put(result.getMetaData().getColumnLabel(i),result.getObject(i));rows.add(row);
            }}
        }
        return rows;
    }
    static final String FIXTURES="""
        WITH fixture_user AS (
            SELECT 7 id,20 dept_id,0 deleted,1 status UNION ALL SELECT 8,20,0,1
            UNION ALL SELECT 9,21,0,1 UNION ALL SELECT 10,20,1,1 UNION ALL SELECT 11,NULL,0,1 UNION ALL SELECT 12,20,0,0
        ), fixture_customer AS (
            SELECT 1 id,'safe' customer_name,'A' customer_level,CAST('2026-01-01 00:00:00.000' AS DATETIME(3)) created_at,7 owner_id,'ACTIVE' status,0 deleted
            UNION ALL SELECT 2,'never',NULL,'2026-01-01',8,'POTENTIAL',0
            UNION ALL SELECT 3,'public','A','2026-01-01',NULL,'ACTIVE',0
            UNION ALL SELECT 4,'inactive','A','2026-01-01',7,'INACTIVE',0
            UNION ALL SELECT 5,'deleted','A','2026-01-01',7,'ACTIVE',1
            UNION ALL SELECT 6,'otherdept','A','2026-01-01',9,'ACTIVE',0
            UNION ALL SELECT 7,'deletedowner','A','2026-01-01',10,'ACTIVE',0
            UNION ALL SELECT 8,'missingowner','A','2026-01-01',999,'ACTIVE',0
            UNION ALL SELECT 9,'nodept','A','2026-01-01',11,'ACTIVE',0
            UNION ALL SELECT 10,'disabledowner','A','2026-01-01',12,'ACTIVE',0
            UNION ALL SELECT 11,'invalidonly','A','2026-01-01',7,'ACTIVE',0
        ), fixture_follow AS (
            SELECT 1 id,'CUSTOMER' target_type,1 target_id,CAST('2026-10-01 00:00:00.000' AS DATETIME(3)) follow_at,0 deleted,9 owner_id
            UNION ALL SELECT 2,'CUSTOMER',1,'2026-10-02',0,8
            UNION ALL SELECT 3,'CLUE',1,'2026-10-17',0,7
            UNION ALL SELECT 4,'CUSTOMER',1,'2026-10-17',1,7
            UNION ALL SELECT 5,'CUSTOMER',1,'2026-10-18 12:00:00.001',0,7
            UNION ALL SELECT 6,'CUSTOMER',1,'2025-12-31',0,7
            UNION ALL SELECT 7,'CUSTOMER',1,NULL,0,7
            UNION ALL SELECT 8,'CUSTOMER',11,NULL,0,7
        )
        """;
    @Test void actualMapperSqlHandlesOwnershipHistoryAndAnomaliesWithoutWrites() throws Exception {
        var params=CustomerRiskMapperTest.parameters(new DataScopeContext(DataScopeType.DEPT,7L,20L),Set.of(),null);
        var rows=query(params,FIXTURES);
        assertThat(rows).extracting(r->((Number)r.get("id")).longValue()).containsExactly(1L,2L,11L);
        var first=rows.get(0);
        assertThat(((Number)first.get("history_count")).longValue()).isEqualTo(5);
        assertThat(((Number)first.get("valid_count")).longValue()).isEqualTo(2);
        assertThat(((Number)first.get("anomalous_count")).longValue()).isEqualTo(3);
        assertThat(first.get("last_valid_follow_at").toString()).startsWith("2026-10-02");
        assertThat(((Number)rows.get(1).get("history_count")).longValue()).isZero();
        assertThat(((Number)rows.get(2).get("anomalous_count")).longValue()).isEqualTo(1);
        params.put("dataScope",new DataScopeContext(DataScopeType.SELF,7L,null));
        assertThat(query(params,FIXTURES)).extracting(r->((Number)r.get("id")).longValue()).containsExactly(1L,11L);
        params.put("customerId",6L);assertThat(query(params,FIXTURES)).isEmpty();
        params.put("customerId",999L);assertThat(query(params,FIXTURES)).isEmpty();
        params.put("dataScope",new DataScopeContext(DataScopeType.ALL,7L,null));params.put("customerId",null);
        assertThat(query(params,FIXTURES)).isEmpty();
    }
    @Test void realMyBatisMapsMinimalProjectionAndUsesReadOnlyTransaction() throws Exception {
        var config=CustomerRiskMapperTest.configuration();
        var dataSource=new org.apache.ibatis.datasource.unpooled.UnpooledDataSource("com.mysql.cj.jdbc.Driver",
                System.getenv("CRM_RISK_TEST_JDBC_URL"),System.getenv("CRM_RISK_TEST_DB_USER"),System.getenv("CRM_RISK_TEST_DB_PASSWORD"));
        config.setEnvironment(new org.apache.ibatis.mapping.Environment("read-only-risk",new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),dataSource));
        try(var session=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config).openSession(connection)) {
            var mapper=session.getMapper(CustomerRiskMapper.class);
            Long owner=null;
            try(var statement=connection.createStatement();var rs=statement.executeQuery("SELECT id FROM sys_user WHERE username='sales_staff03' AND status=1 AND deleted=0")) {
                if(rs.next()) owner=rs.getLong(1);
            }
            Assumptions.assumeTrue(owner!=null,"Local sales staff unavailable");
            var rows=mapper.selectCandidates(new DataScopeContext(DataScopeType.SELF,owner,null),java.time.LocalDateTime.of(2026,10,18,12,0),Set.of(),null,5001);
            assertThat(rows).isNotEmpty().allSatisfy(row->{
                assertThat(row.getId()).isPositive();assertThat(row.getCustomerNo()).isNotBlank();
                assertThat(row.getCustomerStatus()).isIn("POTENTIAL","ACTIVE");
                assertThat(row.getHistoryCount()).isEqualTo(row.getValidCount()+row.getAnomalousCount());
                if(row.getValidCount()>0) assertThat(row.getLastValidFollowAt()).isNotNull();
            });
            assertThat(connection.isReadOnly()).isTrue();
        }
        connection=null;
    }
    @Test void existingDatabaseMatchesIndependentReadOnlyCount() throws Exception {
        Long userId=null,deptId=null;
        try(var statement=connection.createStatement();var rs=statement.executeQuery("SELECT id,dept_id FROM sys_user WHERE username='sales_manager01' AND status=1 AND deleted=0")) {
            if(rs.next()) {userId=rs.getLong(1);deptId=rs.getLong(2);}
        }
        Assumptions.assumeTrue(userId!=null && deptId!=null,"Local sales manager fixture unavailable");
        var rows=query(CustomerRiskMapperTest.parameters(new DataScopeContext(DataScopeType.DEPT,userId,deptId),Set.of(),null),null);
        try(var statement=connection.prepareStatement("SELECT COUNT(*) FROM customer c WHERE c.deleted=0 AND c.status IN ('POTENTIAL','ACTIVE') AND c.owner_id IN (SELECT id FROM sys_user WHERE dept_id=? AND status=1 AND deleted=0)")) {
            statement.setLong(1,deptId);try(var rs=statement.executeQuery()) {rs.next();assertThat(rows.size()).isEqualTo(rs.getLong(1));}
        }
        assertThat(rows).allSatisfy(r->assertThat(((Number)r.get("history_count")).longValue()).isEqualTo(((Number)r.get("valid_count")).longValue()+((Number)r.get("anomalous_count")).longValue()));
    }
}
