package com.company.crm.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisPermissionSqlTest {

    @Test
    void shouldContainIdempotentPermissionAndFourRoleBindings() throws IOException {
        try (var input = getClass().getResourceAsStream("/db/ai-analysis-permission.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql).contains("'ai:analysis'", "'SUPER_ADMIN'", "'SYSTEM_ADMIN'",
                    "'SALES_MANAGER'", "'SALES_STAFF'");
            assertThat(sql).contains("WHERE NOT EXISTS", "existing_relation.role_id = role.id",
                    "existing_relation.permission_id = permission.id");
            assertThat(sql).contains("@ai_analysis_role_count = 4", "permission_type = 'API'",
                    "http_method = 'POST'", "api_path = '/ai/analysis'");
            assertThat(sql).doesNotContain("DELETE FROM sys_role_permission", "TRUNCATE", "DROP TABLE");
        }
    }
}
