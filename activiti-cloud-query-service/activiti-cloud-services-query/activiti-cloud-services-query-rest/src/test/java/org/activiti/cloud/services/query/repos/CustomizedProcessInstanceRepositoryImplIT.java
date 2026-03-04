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

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    void testMapSubprocessesForPage_directChildren() {
        Pageable pageable = PageRequest.of(0, 10);

        // parent1 is its own root; parent2 is its own root
        ProcessInstanceEntity parent1 = buildProcessInstanceEntity();
        parent1.setRootProcessInstanceId(parent1.getId());
        ProcessInstanceEntity parent2 = buildProcessInstanceEntity();
        parent2.setRootProcessInstanceId(parent2.getId());
        // unrelated process — should not appear in any subprocess set
        ProcessInstanceEntity unrelated = buildProcessInstanceEntity();
        unrelated.setRootProcessInstanceId(unrelated.getId());
        processInstanceRepository.saveAll(List.of(parent1, parent2, unrelated));

        buildSubprocessInstances(2, parent1.getId(), parent1.getId());
        buildSubprocessInstances(3, parent2.getId(), parent2.getId());

        entityManager.flush();

        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(
            List.of(parent1, parent2, unrelated),
            pageable,
            3
        );

        Page<ProcessInstanceEntity> result = repository.mapSubprocesses(processInstances, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3);

        ProcessInstanceEntity resultParent1 = result
            .getContent()
            .stream()
            .filter(p -> p.getId().equals(parent1.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(resultParent1.getSubprocesses()).hasSize(2);

        ProcessInstanceEntity resultParent2 = result
            .getContent()
            .stream()
            .filter(p -> p.getId().equals(parent2.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(resultParent2.getSubprocesses()).hasSize(3);

        ProcessInstanceEntity resultUnrelated = result
            .getContent()
            .stream()
            .filter(p -> p.getId().equals(unrelated.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(resultUnrelated.getSubprocesses()).isEmpty();
    }

    @Test
    void testMapSubprocessesForPage_deepNestedSubprocesses() {
        Pageable pageable = PageRequest.of(0, 10);
        String rootId = UUID.randomUUID().toString();

        // root → child → grandchild
        ProcessInstanceEntity root = buildProcessInstanceEntity();
        root.setRootProcessInstanceId(rootId.equals(root.getId()) ? rootId : root.getId());
        // reuse root.id as rootProcessInstanceId (root is its own root)
        root.setRootProcessInstanceId(root.getId());
        processInstanceRepository.save(root);

        ProcessInstanceEntity child = buildProcessInstanceEntity();
        child.setParentId(root.getId());
        child.setRootProcessInstanceId(root.getId());
        processInstanceRepository.save(child);

        ProcessInstanceEntity grandchild = buildProcessInstanceEntity();
        grandchild.setParentId(child.getId());
        grandchild.setRootProcessInstanceId(root.getId());
        processInstanceRepository.save(grandchild);

        entityManager.flush();

        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(List.of(root), pageable, 1);
        Page<ProcessInstanceEntity> result = repository.mapSubprocesses(processInstances, pageable);

        ProcessInstanceEntity resultRoot = result.getContent().getFirst();
        // Should contain both direct child AND grandchild
        assertThat(resultRoot.getSubprocesses())
            .hasSize(2)
            .extracting(sp -> sp.getId())
            .containsExactlyInAnyOrder(child.getId(), grandchild.getId());
    }

    @Test
    void testMapSubprocessesForProcessInstance() {
        ProcessInstanceEntity parent = buildProcessInstanceEntity();
        parent.setRootProcessInstanceId(parent.getId());
        processInstanceRepository.save(parent);

        buildSubprocessInstances(2, parent.getId(), parent.getId());

        entityManager.flush();

        ProcessInstanceEntity result = repository.mapSubprocesses(parent);

        assertThat(result).isNotNull();
        assertThat(result.getSubprocesses()).hasSize(2);
    }

    private List<ProcessInstanceEntity> buildDefaultProcessInstances(int count) {
        List<ProcessInstanceEntity> entities = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            entities.add(buildProcessInstanceEntity());
        }
        processInstanceRepository.saveAll(entities);
        return entities;
    }

    private void buildSubprocessInstances(int count, String parentId, String rootId) {
        List<ProcessInstanceEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ProcessInstanceEntity e = buildProcessInstanceEntity();
            e.setParentId(parentId);
            e.setRootProcessInstanceId(rootId);
            entities.add(e);
        }
        processInstanceRepository.saveAll(entities);
    }

    private void setSubprocesses(List<ProcessInstanceEntity> subprocesses, String parentId) {
        for (ProcessInstanceEntity subprocess : subprocesses) {
            subprocess.setParentId(parentId);
            processInstanceRepository.save(subprocess);
        }
    }

    @Test
    void testMapAllLinkedProcesses() {
        Pageable pageable = PageRequest.of(0, 10);
        List<ProcessInstanceEntity> mainProcesses = buildDefaultProcessInstances(2);
        String mainId1 = mainProcesses.get(0).getId();
        String mainId2 = mainProcesses.get(1).getId();

        // Two linked processes pointing to mainId1, one to mainId2
        buildLinkedProcessInstances(2, mainId1);
        buildLinkedProcessInstances(1, mainId2);

        entityManager.flush();

        Page<ProcessInstanceEntity> processInstances = new PageImpl<>(mainProcesses, pageable, mainProcesses.size());

        Page<ProcessInstanceEntity> result = repository.mapAllLinkedProcesses(processInstances);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);

        ProcessInstanceEntity resultMain1 = result
            .getContent()
            .stream()
            .filter(p -> p.getId().equals(mainId1))
            .findFirst()
            .orElseThrow();
        assertThat(resultMain1.getLinkedProcesses()).hasSize(2);

        ProcessInstanceEntity resultMain2 = result
            .getContent()
            .stream()
            .filter(p -> p.getId().equals(mainId2))
            .findFirst()
            .orElseThrow();
        assertThat(resultMain2.getLinkedProcesses()).hasSize(1);
    }

    private void buildLinkedProcessInstances(int count, String linkedToId) {
        List<ProcessInstanceEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ProcessInstanceEntity entity = buildProcessInstanceEntity();
            entity.setLinkedProcessInstanceId(linkedToId);
            entities.add(entity);
        }
        processInstanceRepository.saveAll(entities);
    }
}
