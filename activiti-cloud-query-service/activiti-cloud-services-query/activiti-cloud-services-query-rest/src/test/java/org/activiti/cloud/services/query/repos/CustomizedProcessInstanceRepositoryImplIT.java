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

package org.activiti.cloud.services.query.repos;

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.services.query.app.repository.CustomizedProcessInstanceRepositoryImpl;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class CustomizedProcessInstanceRepositoryImplIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CustomizedProcessInstanceRepositoryImpl repository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        processInstanceRepository.deleteAll();
    }

    @Test
    void testMapSubprocessesForPage() {
        Pageable pageable = PageRequest.of(0, 10);
        List<ProcessInstanceEntity> processInstancesList = buildDefaultProcessInstances(3);
        String parentId1 = processInstancesList.getFirst().getId();
        List<ProcessInstanceEntity> subprocesses1 = buildDefaultProcessInstances(2);
        setSubprocesses(subprocesses1, parentId1);
        String parentId2 = processInstancesList.getLast().getId();
        List<ProcessInstanceEntity> subprocesses2 = buildDefaultProcessInstances(3);
        setSubprocesses(subprocesses2, parentId2);

        entityManager.flush();

        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(
            processInstancesList,
            pageable,
            processInstancesList.size()
        );

        Page<ProcessInstanceEntity> result = repository.mapSubprocesses(processInstances, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);

        ProcessInstanceEntity parentInstance1 = result
            .getContent()
            .stream()
            .filter(instance -> instance.getId().equals(parentId1))
            .findFirst()
            .orElse(null);
        assertThat(parentInstance1).isNotNull();
        assertThat(parentInstance1.getSubprocesses()).hasSize(2);

        ProcessInstanceEntity parentInstance2 = result
            .getContent()
            .stream()
            .filter(instance -> instance.getId().equals(parentId2))
            .findFirst()
            .orElse(null);
        assertThat(parentInstance2).isNotNull();
        assertThat(parentInstance2.getSubprocesses()).hasSize(3);
    }

    @Test
    void testMapSubprocessesForProcessInstance() {
        List<ProcessInstanceEntity> processInstances = buildDefaultProcessInstances(5);
        List<ProcessInstanceEntity> subprocesses = buildDefaultProcessInstances(2);
        ProcessInstanceEntity entity = processInstances.getFirst();
        String parentId = entity.getId();
        setSubprocesses(subprocesses, parentId);

        ProcessInstanceEntity result = repository.mapSubprocesses(entity);

        assertThat(result).isNotNull();
        assertThat(result.getSubprocesses().stream().toList().getLast().getId()).isNotNull();
    }

    private List<ProcessInstanceEntity> buildDefaultProcessInstances(int count) {
        List<ProcessInstanceEntity> entities = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            entities.add(buildProcessInstanceEntity());
        }
        processInstanceRepository.saveAll(entities);
        return entities;
    }

    private void setSubprocesses(List<ProcessInstanceEntity> subprocesses, String parentId) {
        for (ProcessInstanceEntity subprocess : subprocesses) {
            subprocess.setParentId(parentId);
            processInstanceRepository.save(subprocess);
        }
    }
}
