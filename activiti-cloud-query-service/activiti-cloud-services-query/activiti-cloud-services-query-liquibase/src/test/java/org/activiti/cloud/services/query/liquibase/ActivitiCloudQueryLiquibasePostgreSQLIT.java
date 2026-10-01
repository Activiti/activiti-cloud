/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.liquibase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = ActivitiCloudQueryLiquibaseAutoConfigurationIT.class)
@Testcontainers
class ActivitiCloudQueryLiquibasePostgreSQLIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldCreateRootUnlinkedStartDateIndexForProcessInstance() {
        var index = jdbcTemplate.queryForMap(
            """
            SELECT pg_get_indexdef(indexrelid), pg_get_expr(indpred, indrelid)
            FROM pg_index
            WHERE indexrelid = 'pi_root_unlinked_startdate_idx'::regclass
            """
        );

        assertThat(index.get("pg_get_indexdef", String.class)).contains("start_date DESC NULLS LAST");
        assertThat(index.get("pg_get_expr", String.class)).isEqualTo(
            "((parent_id IS NULL) AND ((linked_process_instance_id IS NULL) OR (linked_process_instance_type IS NULL)))"
        );
    }
}
