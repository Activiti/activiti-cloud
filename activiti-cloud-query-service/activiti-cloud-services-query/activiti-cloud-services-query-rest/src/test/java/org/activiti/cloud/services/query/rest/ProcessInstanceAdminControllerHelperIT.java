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
package org.activiti.cloud.services.query.rest;

import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntity;
import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.buildProcessInstanceEntityWithLinkedProcess;
import static org.activiti.cloud.services.query.util.ProcessInstanceTestUtils.createProcessVariables;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.querydsl.core.types.Predicate;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceAdminControllerHelper;
import org.activiti.core.common.spring.security.policies.ActivitiForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
@Testcontainers
class ProcessInstanceAdminControllerHelperIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    ProcessInstanceAdminControllerHelper processInstanceAdminControllerHelper;

    @Autowired
    ProcessInstanceRepository processInstanceRepository;

    @BeforeEach
    void setUp() {
        processInstanceRepository.deleteAll();
    }

    @Test
    void shouldReturnAllProcessInstanceAdmin() {
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        processInstanceRepository.save(processInstanceEntity);
        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = getPageableSortedByLastModifiedDescending(pageSize);

        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdmin(
            predicate,
            pageable
        );

        assertThat(result.getContent()).contains(processInstanceEntity);

        ProcessInstanceEntity returnedProcessInstance = verifyReturnedProcessInstanceEntity(
            result,
            processInstanceEntity
        );

        assertThat(returnedProcessInstance).isNotNull();
        assertThat(returnedProcessInstance.getSubprocesses()).isEmpty();
    }

    @Test
    void shouldReturnAllProcessInstanceAdminWithSubprocess() {
        ProcessInstanceEntity parentProcessInstance = buildProcessInstanceEntity();
        ProcessInstanceEntity subprocessInstance = buildSubprocessInstance(parentProcessInstance);

        processInstanceRepository.save(parentProcessInstance);
        processInstanceRepository.save(subprocessInstance);

        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = getPageableSortedByLastModifiedDescending(pageSize);

        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdmin(
            predicate,
            pageable
        );

        assertThat(result.getContent()).contains(parentProcessInstance, subprocessInstance);

        ProcessInstanceEntity returnedParentProcessInstance = verifyReturnedProcessInstanceEntity(
            result,
            parentProcessInstance
        );

        assertThat(returnedParentProcessInstance).isNotNull();
        assertThat(returnedParentProcessInstance.getSubprocesses())
            .anyMatch(subprocess ->
                subprocess.getId().equals(subprocessInstance.getId()) &&
                subprocess.getProcessDefinitionName().equals(subprocessInstance.getProcessDefinitionName())
            );
    }

    @Test
    void shouldReturnAllProcessInstanceAdminWithVariables() {
        ProcessInstanceEntity processInstanceEntity = buildProcessInstanceEntity();
        processInstanceRepository.save(processInstanceEntity);

        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity, 8);
        List<String> variableKeys = variables.stream().map(ProcessVariableEntity::getName).toList();
        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = getPageableSortedByLastModifiedDescending(pageSize);

        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdminWithVariables(
            predicate,
            variableKeys,
            pageable
        );

        assertThat(result.getContent()).contains(processInstanceEntity);
    }

    @Test
    void shouldReturnProcessAdminById() {
        ProcessInstanceEntity parentProcessInstance = buildProcessInstanceEntity();
        ProcessInstanceEntity subprocessInstance = buildSubprocessInstance(parentProcessInstance);

        processInstanceRepository.save(parentProcessInstance);
        processInstanceRepository.save(subprocessInstance);
        String processInstanceId = parentProcessInstance.getId();

        ProcessInstanceEntity result = processInstanceAdminControllerHelper.findByIdProcessAdmin(processInstanceId);

        assertThat(result).isEqualTo(parentProcessInstance);
        assertThat(
            result
                .getSubprocesses()
                .stream()
                .anyMatch(subprocess -> subprocess.getId().equals(subprocessInstance.getId()))
        )
            .isTrue();
    }

    private ProcessInstanceEntity buildSubprocessInstance(ProcessInstanceEntity parentProcessInstance) {
        ProcessInstanceEntity subprocessInstance = new ProcessInstanceEntity();
        subprocessInstance.setId(UUID.randomUUID().toString());
        subprocessInstance.setProcessDefinitionKey("mySubprocess");
        subprocessInstance.setProcessDefinitionName("subprocess");
        subprocessInstance.setStatus(ProcessInstance.ProcessInstanceStatus.RUNNING);
        subprocessInstance.setParentId(parentProcessInstance.getId());
        return subprocessInstance;
    }

    private ProcessInstanceEntity verifyReturnedProcessInstanceEntity(
        Page<ProcessInstanceEntity> result,
        ProcessInstanceEntity parentProcessInstance
    ) {
        return result
            .getContent()
            .stream()
            .filter(pi -> pi.getId().equals(parentProcessInstance.getId()))
            .findFirst()
            .orElse(null);
    }

    private PageRequest getPageableSortedByLastModifiedDescending(int pageSize) {
        return PageRequest.of(0, pageSize, Sort.by("lastModified").descending());
    }

    @Test
    @WithMockUser(roles = "ACTIVITI_ADMIN")
    void shouldReturnLinkedProcessesAdminById() {
        ProcessInstanceEntity linkedProcessInstance = buildProcessInstanceEntity();
        var savedLinkedProcessInstance = processInstanceRepository.save(linkedProcessInstance);
        var saveLinkedProcessInstanceId = savedLinkedProcessInstance.getId();
        ProcessInstanceEntity processInstance1 = buildProcessInstanceEntityWithLinkedProcess(
            saveLinkedProcessInstanceId
        );
        processInstanceRepository.save(processInstance1);
        ProcessInstanceEntity processInstance2 = buildProcessInstanceEntityWithLinkedProcess(
            saveLinkedProcessInstanceId
        );
        processInstanceRepository.save(processInstance2);
        ProcessInstanceEntity processInstance3 = buildProcessInstanceEntityWithLinkedProcess(
            saveLinkedProcessInstanceId
        );
        processInstanceRepository.save(processInstance3);

        int pageSize = 30;
        Pageable pageable = getPageableSortedByLastModifiedDescending(pageSize);

        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.searchLinkedProcesses(
            saveLinkedProcessInstanceId,
            pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .extracting("id")
            .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId(), processInstance3.getId());
    }

    @Test
    @WithMockUser(roles = "ACTIVITI_ADMIN")
    void shouldReturnEmptyListWhenGetLinkedProcessesAdminById() {
        ProcessInstanceEntity linkedProcessInstance = buildProcessInstanceEntity();
        var savedLinkedProcessInstance = processInstanceRepository.save(linkedProcessInstance);
        var saveLinkedProcessInstanceId = savedLinkedProcessInstance.getId();

        int pageSize = 30;
        Pageable pageable = getPageableSortedByLastModifiedDescending(pageSize);

        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.searchLinkedProcesses(
            saveLinkedProcessInstanceId,
            pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @WithMockUser(roles = { "ACTIVITI_USER", "ACTIVITI_MODELER" })
    void shouldThrowForbiddenWhenGetLinkedProcessesAdminById() {
        ProcessInstanceEntity linkedProcessInstance = buildProcessInstanceEntity();
        var savedLinkedProcessInstance = processInstanceRepository.save(linkedProcessInstance);
        var saveLinkedProcessInstanceId = savedLinkedProcessInstance.getId();

        int pageSize = 30;
        Pageable pageable = getPageableSortedByLastModifiedDescending(pageSize);

        assertThatThrownBy(() ->
                processInstanceAdminControllerHelper.searchLinkedProcesses(saveLinkedProcessInstanceId, pageable)
            )
            .isInstanceOf(ActivitiForbiddenException.class)
            .hasMessageContaining("Operation not permitted for process instance: " + saveLinkedProcessInstanceId);
    }

    @Test
    @WithMockUser(roles = "ACTIVITI_ADMIN")
    void shouldThrowEntityNotFoundExceptionWhenGetLinkedProcessesAdminById() {
        int pageSize = 30;
        Pageable pageable = getPageableSortedByLastModifiedDescending(pageSize);

        assertThatThrownBy(() -> processInstanceAdminControllerHelper.searchLinkedProcesses("linkedProcessId", pageable)
            )
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Unable to find process for the given id:'linkedProcessId'");
    }
}
