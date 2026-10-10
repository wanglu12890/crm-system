package com.company.crm.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the security-sensitive predicates in the dynamic list query. */
class CustomerMapperSqlTest {

    @Test
    void shouldKeepBasePublicPoolAndDataScopePredicatesInSingleQuery() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mapper/CustomerMapper.xml")) {
            assertThat(input).isNotNull();
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(xml).contains("c.deleted = 0");
            assertThat(xml).contains("c.owner_id IS NOT NULL");
            assertThat(xml).contains("owner.dept_id = #{dataScope.deptId}");
            assertThat(xml).contains("c.owner_id = #{dataScope.userId}");
            assertThat(xml).contains("c.owner_id = #{query.ownerId}");
            assertThat(xml).contains("<sql id=\"customerVisibilityConditions\">");
            assertThat(xml).contains("<include refid=\"customerVisibilityConditions\"/>");
        }
    }

    @Test
    void shouldUseOneAggregateQueryWithControlledTimeAndLevelConditions() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/mapper/CustomerMapper.xml")) {
            assertThat(input).isNotNull();
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(xml).contains("selectCustomerStatistics");
            assertThat(xml).contains("COUNT(*)");
            assertThat(xml).contains("c.customer_level = 'A'");
            assertThat(xml).contains("c.customer_level = 'B'");
            assertThat(xml).contains("c.customer_level = 'C'");
            assertThat(xml).contains("c.customer_level = 'D'");
            assertThat(xml).contains("c.customer_level IS NULL");
            assertThat(xml).contains("c.created_at &gt;= #{startTime}");
            assertThat(xml).contains("c.created_at &lt; #{endTime}");
            assertThat(xml).doesNotContain("NOW()", "CURDATE()");
        }
    }
}
