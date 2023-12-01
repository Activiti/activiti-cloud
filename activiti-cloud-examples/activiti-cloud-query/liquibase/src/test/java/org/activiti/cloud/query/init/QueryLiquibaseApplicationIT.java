/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.activiti.cloud.query.liquibase.QueryLiquibaseApplication;
import org.activiti.cloud.services.audit.jpa.events.AuditEventEntity;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = { QueryLiquibaseApplication.class }, properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
@EntityScan(basePackageClasses = { ProcessInstanceEntity.class, AuditEventEntity.class })
public class QueryLiquibaseApplicationIT {

    @MockBean
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    public void contextLoads() {
        assertThat(entityManager.getMetamodel().getEntities())
            .hasSizeGreaterThan(0)
            .extracting(EntityType::getName)
            .contains("AuditEvent", "ProcessInstance");
    }
}
