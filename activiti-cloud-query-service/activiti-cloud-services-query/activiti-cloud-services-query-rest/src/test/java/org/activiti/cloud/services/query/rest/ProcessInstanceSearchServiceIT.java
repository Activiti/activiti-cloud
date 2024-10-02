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
import org.activiti.cloud.services.query.util.QueryTestUtils;
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
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
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

    @Autowired
    private QueryTestUtils queryTestUtils;

    @BeforeEach
    public void setUp() {
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
    }

    @Test
    void should_return_AllProcessInstances() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("another-user")
            .buildAndSave();

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest();

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactlyInAnyOrder(processInstance1, processInstance2);
    }

    @Test
    void should_return_restrictedProcessInstances() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("another-user")
            .buildAndSave();
        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("another-user")
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers(USER))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("another-user").buildAndSave();

        ProcessInstanceSearchRequest processInstanceSearchRequest = buildProcessInstanceSearchRequest();

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchRestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactlyInAnyOrder(processInstance1, processInstance2, processInstance3);
    }

    @Test
    void should_returnProcessInstances_filteredByNameLike() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withName("Beautiful process instance name")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withName("Amazing process instance name")
            .buildAndSave();

        ProcessInstanceSearchRequest processInstanceSearchRequest = new ProcessInstanceSearchRequest(
            Set.of("beautiful", "amazing"),
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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1, processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByInitiator() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user1")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user2")
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("user3").buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1, processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByAppVersion() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withAppVersion("1.0.0")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withAppVersion("2.0.0")
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withAppVersion("3.0.0").buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1, processInstance2);
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withLastModified(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withLastModified(new Date(1000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withLastModified(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withLastModified(new Date(2000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByStartFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withStartDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withStartDate(new Date(1000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByStartTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withStartDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withStartDate(new Date(2000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withCompletedDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withCompletedDate(new Date(1000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withCompletedDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withCompletedDate(new Date(2000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withSuspendedDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withSuspendedDate(new Date(1000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withSuspendedDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withSuspendedDate(new Date(2000)).buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances).containsExactly(processInstance1);
    }

    @Test
    void should_returnProcessInstance_withoutProcessVariables() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances)
            .containsExactly(processInstance)
            .first()
            .extracting(ProcessInstanceEntity::getVariables)
            .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
            .isEmpty();
    }

    @Test
    void should_returnProcessInstances_withJustRequestedProcessVariables() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.INTEGER, 1),
                new QueryTestUtils.VariableInput("var3", VariableType.STRING, "value3"),
                new QueryTestUtils.VariableInput("var4", VariableType.BOOLEAN, true)
            )
            .buildAndSave();

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

        List<ProcessInstanceEntity> retrievedInstances = processInstanceSearchService
            .searchUnrestricted(processInstanceSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .toList();

        assertThat(retrievedInstances)
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
}
