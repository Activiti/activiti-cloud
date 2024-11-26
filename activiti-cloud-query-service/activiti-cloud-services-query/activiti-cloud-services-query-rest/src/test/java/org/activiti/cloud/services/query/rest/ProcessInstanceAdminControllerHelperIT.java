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
package org.activiti.cloud.services.query.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.types.Predicate;
import java.util.*;
import java.util.stream.Collectors;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.cloud.services.query.app.repository.*;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceAdminControllerHelper;
import org.activiti.cloud.services.query.rest.helper.ProcessInstanceControllerHelper;
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
public class ProcessInstanceAdminControllerHelperIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    ProcessInstanceControllerHelper processInstanceControllerHelper;

    @Autowired
    ProcessInstanceAdminControllerHelper processInstanceAdminControllerHelper;

    @Autowired
    ProcessInstanceRepository processInstanceRepository;

    @BeforeEach
    public void setUp() {
        processInstanceRepository.deleteAll();
    }

    @Test
    public void shouldReturnAllProcessInstanceAdmin() {
        ProcessInstanceEntity processInstanceEntity = buildDefaultProcessInstance();
        processInstanceRepository.save(processInstanceEntity);
        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("lastModified").descending());

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
    public void shouldReturnAllProcessInstanceAdminWithSubprocess() {
        ProcessInstanceEntity parentProcessInstance = buildDefaultProcessInstance();
        ProcessInstanceEntity subprocessInstance = buildSubprocessInstance(parentProcessInstance);

        processInstanceRepository.save(parentProcessInstance);
        processInstanceRepository.save(subprocessInstance);

        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("lastModified").descending());

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
    public void shouldReturnAllProcessInstanceAdminWithVariables() {
        ProcessInstanceEntity processInstanceEntity = buildDefaultProcessInstance();
        processInstanceRepository.save(processInstanceEntity);

        Set<ProcessVariableEntity> variables = createProcessVariables(processInstanceEntity, 8);
        List<String> variableKeys = variables.stream().map(ProcessVariableEntity::getName).collect(Collectors.toList());
        Predicate predicate = null;
        int pageSize = 30;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("lastModified").descending());

        Page<ProcessInstanceEntity> result = processInstanceAdminControllerHelper.findAllProcessInstanceAdminWithVariables(
            predicate,
            variableKeys,
            pageable
        );

        assertThat(result.getContent()).contains(processInstanceEntity);
    }

    @Test
    public void shouldReturnProcessAdminById() {
        ProcessInstanceEntity parentProcessInstance = buildDefaultProcessInstance();
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

    private ProcessInstanceEntity buildDefaultProcessInstance() {
        return new ProcessInstanceEntity(
            "My-app",
            "My-app",
            "1",
            null,
            null,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            ProcessInstance.ProcessInstanceStatus.RUNNING,
            new Date()
        );
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

    private Set<ProcessVariableEntity> createProcessVariables(
        ProcessInstanceEntity processInstanceEntity,
        int numberOfVariables
    ) {
        Set<ProcessVariableEntity> variables = new HashSet<>();

        for (int i = 0; i < numberOfVariables; i++) {
            ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
            processVariableEntity.setName("name" + i);
            processVariableEntity.setValue("id");
            processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
            processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
            processVariableEntity.setProcessInstance(processInstanceEntity);
            variables.add(processVariableEntity);
        }
        return variables;
    }
}
