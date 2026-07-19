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
package org.activiti.cloud.query.init;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.activiti.cloud.query.liquibase.QueryLiquibaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    classes = { QueryLiquibaseApplication.class, QueryLiquibaseApplicationIT.TestEntityScanConfig.class },
    properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@Testcontainers
public class QueryLiquibaseApplicationIT {

    @Configuration
    @EntityScan(
        basePackages = { "org.activiti.cloud.services.query.model", "org.activiti.cloud.services.audit.jpa.events" }
    )
    static class TestEntityScanConfig {}

    @MockitoBean
    private JsonMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Test
    public void contextLoads() {
        assertThat(entityManager.getMetamodel().getEntities())
            .hasSizeGreaterThan(0)
            .extracting(EntityType::getName)
            .contains("AuditEvent", "ProcessInstance")
            .doesNotContain("ProcessVariableHistory");
    }
}
