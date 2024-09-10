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

import static org.activiti.cloud.services.query.rest.ProcessInstanceSearchServiceIT.USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.ProcessInstanceSearchRequest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@Testcontainers
@TestPropertySource("classpath:application-test.properties")
@WithMockUser(USER)
class ProcessInstanceSearchServiceIT {

    private static final String PROCESS_DEFINITION_KEY = "process-def-key";

    public static final String USER = "testuser";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    ProcessInstanceSearchService processInstanceSearchService;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Autowired
    private SecurityManager securityManager;

    @BeforeEach
    public void setUp() {
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
    }

    @Test
    void should_return_AllProcessInstances() {
        ProcessInstanceEntity processInstance1 = createProcessInstance("process1", Map.of());
        ProcessInstanceEntity processInstance2 = createProcessInstance("process2", Map.of());

        processInstance2.setInitiator("another-user");
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest();

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_return_restrictedProcessInstances() {
        ProcessInstanceEntity processInstance1 = createProcessInstance("process1", Map.of());
        ProcessInstanceEntity processInstance2 = createProcessInstance("process2", Map.of());
        ProcessInstanceEntity processInstance3 = createProcessInstance("process3", Map.of());
        ProcessInstanceEntity processInstance4 = createProcessInstance("process4", Map.of());

        TaskEntity process2Task = new TaskEntity();
        process2Task.setId(UUID.randomUUID().toString());
        process2Task.setProcessInstanceId(processInstance2.getId());
        process2Task.setAssignee(USER);
        taskRepository.save(process2Task);

        processInstance2.setInitiator("another-user");
        processInstance2.setTasks(Set.of(process2Task));
        processInstanceRepository.save(processInstance2);

        TaskEntity process3Task = new TaskEntity();
        process3Task.setId(UUID.randomUUID().toString());
        process3Task.setProcessInstanceId(processInstance3.getId());
        process3Task.setProcessInstance(processInstance3);
        TaskCandidateUserEntity taskCandidate = new TaskCandidateUserEntity();
        taskCandidate.setUserId(USER);
        taskCandidate.setTaskId(process3Task.getId());
        taskCandidate.setTask(process3Task);
        process3Task.setTaskCandidateUsers(Set.of(taskCandidate));
        taskCandidateUserRepository.save(taskCandidate);
        taskRepository.save(process3Task);

        processInstance3.setInitiator("another-user");
        processInstance3.setTasks(Set.of(process3Task));
        processInstanceRepository.save(processInstance3);

        processInstance4.setInitiator("another-user");
        processInstanceRepository.save(processInstance4);

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest();

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchRestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2, processInstance3);
    }

    @Test
    void should_returnProcessInstance_filteredByVariable_whenAllFiltersMatch() {
        ProcessInstanceEntity processInstance = createProcessInstance(
            "process-definition-key",
            Map.of("var1", "value1", "var2", "value2")
        );

        VariableFilter matchingFilter1 = new VariableFilter(
            processInstance.getProcessDefinitionKey(),
            "var1",
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        VariableFilter matchingFilter2 = new VariableFilter(
            processInstance.getProcessDefinitionKey(),
            "var2",
            VariableType.STRING,
            "value2",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(
            matchingFilter1,
            matchingFilter2
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance);
    }

    @Test
    void should_not_returnProcessInstance_filteredByVariable_whenOneFilterDoesNotMatch() {
        ProcessInstanceEntity processInstance = createProcessInstance(
            "process-definition-key",
            Map.of("var1", "value1", "var2", "value2")
        );
        VariableFilter matchingFilter = new VariableFilter(
            processInstance.getProcessDefinitionKey(),
            "var1",
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        VariableFilter notMatchingFilter = new VariableFilter(
            processInstance.getProcessDefinitionKey(),
            "var2",
            VariableType.STRING,
            "value3",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(
            matchingFilter,
            notMatchingFilter
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).isEmpty();
    }

    @Test
    void should_returnTasks_filteredByStringVariable_exactMatch() {
        String processDefinitionKey = "process-def-key";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey, Map.of("var1", "value1"));
        createProcessInstance(processDefinitionKey, Map.of("var1", "other-value"));

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            "var1",
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByStringVariable_Contains() {
        String processDefinitionKey = "process-def-key";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey, Map.of("var1", "abcdefg"));
        createProcessInstance(processDefinitionKey, Map.of("var1", "other-value"));

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            "var1",
            VariableType.STRING,
            "bcde",
            FilterOperator.LIKE
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByIntegerVariable_equals() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 1));

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 2));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.INTEGER,
            String.valueOf(1),
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByIntegerVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 10));

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 2));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.INTEGER,
            String.valueOf(2),
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByIntegerVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 10));

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 2));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.INTEGER,
            String.valueOf(2),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByIntegerVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 2));

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 10));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.INTEGER,
            String.valueOf(10),
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByIntegerVariable_lessThanEqual() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 2));

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", 10));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.INTEGER,
            String.valueOf(10),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalVariable_equals() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", new BigDecimal("1.1"))
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", new BigDecimal("1.2")));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.BIGDECIMAL,
            "1.1",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", new BigDecimal("10.1"))
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", new BigDecimal("2.1")));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.BIGDECIMAL,
            "2.1",
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", new BigDecimal("10.1"))
        );

        ProcessInstanceEntity processInstance2 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", new BigDecimal("2.1"))
        );

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.BIGDECIMAL,
            "2.1",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", new BigDecimal("2.1"))
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", new BigDecimal("10.1")));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.BIGDECIMAL,
            "10.1",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalVariable_lessThanEqual() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", new BigDecimal("2.1"))
        );

        ProcessInstanceEntity processInstance2 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", new BigDecimal("10.1"))
        );

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.BIGDECIMAL,
            "10.1",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByDateVariable_equals() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01")
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", "2024-09-02"));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByDateVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-02")
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", "2024-09-01"));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByDateVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-02")
        );

        ProcessInstanceEntity processInstance2 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01")
        );

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByDateVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01")
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", "2024-09-02"));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATE,
            "2024-09-02",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByDateVariable_lessThanEquals() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01")
        );

        ProcessInstanceEntity processInstance2 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-02")
        );

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATE,
            "2024-09-02",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByDatetimeVariable_equals() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01T00:11:00.000+00:00")
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", "2024-09-01T00:12:00.000+00:00"));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByDatetimeVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01T00:12:00.000+00:00")
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", "2024-09-01T00:11:00.000+00:00"));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByDatetimeVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01T00:12:00.000+00:00")
        );

        ProcessInstanceEntity processInstance2 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01T00:11:00.000+00:00")
        );

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByDatetimeVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01T00:11:00.000+00:00")
        );

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", "2024-09-01T00:12:00.000+00:00"));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATETIME,
            "2024-09-01T00:12:00.000+00:00",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnTasks_filteredByDatetimeVariable_lessThanEqual() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01T00:11:00.000+00:00")
        );

        ProcessInstanceEntity processInstance2 = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "2024-09-01T00:12:00.000+00:00")
        );

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.DATETIME,
            "2024-09-01T00:12:00.000+00:00",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_returnTasks_filteredByBooleanVariable() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", true));

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of("var1", false));

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.BOOLEAN,
            "true",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest(variableFilter);

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByNameLike() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        String name1LikeFilter = processInstance1.getName().substring(1, processInstance1.getName().length() - 1);
        String name2LikeFilter = processInstance2.getName().substring(1, processInstance2.getName().length() - 1);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            Set.of(name1LikeFilter, name2LikeFilter),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1, processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByInitiator() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());
        processInstance2.setInitiator("user2");
        processInstanceRepository.save(processInstance2);

        ProcessInstanceEntity processInstance3 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());
        processInstance3.setInitiator("user3");
        processInstanceRepository.save(processInstance3);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            Set.of(processInstance1.getInitiator(), processInstance2.getInitiator()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1, processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByAppVersion() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            Set.of(processInstance1.getAppVersion(), processInstance2.getAppVersion()),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1, processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedFrom() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setLastModified(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setLastModified(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            new Date(1000),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedTo() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setLastModified(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setLastModified(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            new Date(2000),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByStartFrom() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setStartDate(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setStartDate(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            new Date(1000),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByStartTo() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setStartDate(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setStartDate(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            new Date(2000),
            null,
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedFrom() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setCompletedDate(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setCompletedDate(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new Date(1000),
            null,
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedTo() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setCompletedDate(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setCompletedDate(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new Date(2000),
            null,
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedFrom() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setSuspendedDate(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setSuspendedDate(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new Date(1000),
            null,
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedTo() {
        ProcessInstanceEntity processInstance1 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance1.setSuspendedDate(new Date(1000));
        processInstanceRepository.save(processInstance1);

        ProcessInstanceEntity processInstance2 = createProcessInstance(PROCESS_DEFINITION_KEY, Map.of());

        processInstance2.setSuspendedDate(new Date(2000));
        processInstanceRepository.save(processInstance2);

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new Date(2000),
            null,
            null
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstance_withoutProcessVariables() {
        ProcessInstanceEntity processInstance = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "value1", "var2", "value2")
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Set.of()
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance)
            .first()
            .extracting(ProcessInstanceEntity::getVariables)
            .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
            .isEmpty();
    }

    @Test
    void should_returnProcessInstances_withJustRequestedProcessVariables() {
        ProcessInstanceEntity processInstance = createProcessInstance(
            PROCESS_DEFINITION_KEY,
            Map.of("var1", "value1", "var2", "value2", "var3", "value3", "var4", "value4")
        );

        Set<ProcessVariableKey> processVariableKeys = Set.of(
            new ProcessVariableKey(PROCESS_DEFINITION_KEY, "var1"),
            new ProcessVariableKey(PROCESS_DEFINITION_KEY, "var3")
        );

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            processVariableKeys
        );

        List<ProcessInstanceEntity> retrievedTasks = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance)
            .first()
            .satisfies(process ->
                assertThat(process.getVariables())
                    .satisfiesExactlyInAnyOrder(
                        variable -> {
                            assertThat(variable.getName()).isEqualTo("var1");
                            assertThat((String) variable.getValue()).isEqualTo("value1");
                        },
                        variable -> {
                            assertThat(variable.getName()).isEqualTo("var3");
                            assertThat((String) variable.getValue()).isEqualTo("value3");
                        }
                    )
            );
    }

    private static ProcessInstanceSearchRequest buildProcessInstanceSearchRequest(VariableFilter... variableFilters) {
        Set<ProcessVariableKey> processVariableKeys = Arrays
            .stream(variableFilters)
            .map(variableFilter -> new ProcessVariableKey(variableFilter.processDefinitionKey(), variableFilter.name()))
            .collect(Collectors.toSet());
        return new ProcessInstanceSearchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Set.of(variableFilters),
            processVariableKeys
        );
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance(String processDefKey, Map<String, Object> variables) {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId(UUID.randomUUID().toString());
        processInstanceEntity.setName(UUID.randomUUID().toString());
        processInstanceEntity.setInitiator(USER);
        processInstanceEntity.setProcessDefinitionKey(processDefKey);
        processInstanceEntity.setAppVersion(UUID.randomUUID().toString());
        processInstanceRepository.save(processInstanceEntity);

        Set<ProcessVariableEntity> processVariables = variables
            .entrySet()
            .stream()
            .map(entry -> {
                ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
                processVariableEntity.setName(entry.getKey());
                processVariableEntity.setValue(entry.getValue());
                processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
                processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
                variableRepository.save(processVariableEntity);
                return processVariableEntity;
            })
            .collect(Collectors.toSet());

        processInstanceEntity.setVariables(processVariables);
        return processInstanceRepository.save(processInstanceEntity);
    }
}
