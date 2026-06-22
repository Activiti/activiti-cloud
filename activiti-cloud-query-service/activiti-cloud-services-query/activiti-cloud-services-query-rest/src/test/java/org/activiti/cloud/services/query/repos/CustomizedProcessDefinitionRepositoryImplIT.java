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
package org.activiti.cloud.services.query.repos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.querydsl.core.BooleanBuilder;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.services.query.app.repository.ProcessDefinitionRepository;
import org.activiti.cloud.services.query.model.ProcessDefinitionEntity;
import org.activiti.cloud.services.query.model.QProcessDefinitionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    classes = { QueryRestTestApplication.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@Transactional
class CustomizedProcessDefinitionRepositoryImplIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .waitingFor(Wait.forListeningPort());

    @Autowired
    private ProcessDefinitionRepository processDefinitionRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        processDefinitionRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoData() {
        List<ProcessDefinitionEntity> result = processDefinitionRepository.findAllLatestVersions(new BooleanBuilder());

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSingleEntryPerKeyPickingMaxVersion() {
        ProcessDefinitionEntity firstKeyV1 = buildProcessDefinition("myFirstProcess", 1);
        ProcessDefinitionEntity firstKeyV3 = buildProcessDefinition("myFirstProcess", 3);
        ProcessDefinitionEntity firstKeyV2 = buildProcessDefinition("myFirstProcess", 2);
        ProcessDefinitionEntity secondKeyV1 = buildProcessDefinition("mySecondProcess", 1);
        processDefinitionRepository.saveAll(List.of(firstKeyV1, firstKeyV3, firstKeyV2, secondKeyV1));
        entityManager.flush();

        List<ProcessDefinitionEntity> result = processDefinitionRepository.findAllLatestVersions(new BooleanBuilder());

        assertThat(result)
            .extracting(ProcessDefinitionEntity::getId, ProcessDefinitionEntity::getKey, ProcessDefinitionEntity::getVersion)
            .containsExactlyInAnyOrder(
                tuple(firstKeyV3.getId(), "myFirstProcess", 3),
                tuple(secondKeyV1.getId(), "mySecondProcess", 1)
            );
    }

    @Test
    void shouldCombineDeduplicationWithPredicate() {
        ProcessDefinitionEntity firstKeyV1 = buildProcessDefinition("myFirstProcess", 1);
        ProcessDefinitionEntity firstKeyV2 = buildProcessDefinition("myFirstProcess", 2);
        ProcessDefinitionEntity secondKeyV1 = buildProcessDefinition("mySecondProcess", 1);
        processDefinitionRepository.saveAll(List.of(firstKeyV1, firstKeyV2, secondKeyV1));
        entityManager.flush();

        List<ProcessDefinitionEntity> result = processDefinitionRepository.findAllLatestVersions(
            QProcessDefinitionEntity.processDefinitionEntity.key.eq("myFirstProcess")
        );

        assertThat(result)
            .extracting(ProcessDefinitionEntity::getId, ProcessDefinitionEntity::getKey, ProcessDefinitionEntity::getVersion)
            .containsExactly(tuple(firstKeyV2.getId(), "myFirstProcess", 2));
    }

    private ProcessDefinitionEntity buildProcessDefinition(String key, int version) {
        ProcessDefinitionEntity entity = new ProcessDefinitionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setKey(key);
        entity.setName(key + " v" + version);
        entity.setVersion(version);
        return entity;
    }
}
