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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.util.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
)
@Testcontainers
@TestPropertySource("classpath:application-test.properties")
public class TaskSearchIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    TaskControllerHelper taskControllerHelper;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_exactMatch() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "string-var";
        String valueToSearch = "string-value";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.STRING, valueToSearch);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.STRING, "different-string-value");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.STRING, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task -> {
                assertThat(task.getProcessInstanceId()).isEqualTo(processInstance1.getId());
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(valueToSearch));
            });
    }

    @Test
    void should_returnTasks_filteredByTaskProcessVariable_exactMatch() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "task-var";
        String valueToSearch = "task-value";
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.STRING, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.STRING, "different-value");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_contains() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "string-var";
        String valueToSearch = "jaeger";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.STRING, "Eren Jaeger");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.STRING, "Frank Jaeger");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.STRING, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.CONTAINS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv ->
                        pv.getName().equals(varName) &&
                        ((String) pv.getValue()).toLowerCase().contains(valueToSearch.toLowerCase())
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByTaskProcessVariable_contains() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "task-var";
        String valueToSearch = "fox";
        QueryCloudTask task1 = createTaskWithVariable(processInstance, varName, VariableType.STRING, "Gray Fox");
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.STRING, "Fox Hound");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.CONTAINS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "int-var";
        int valueToSearch = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, valueToSearch);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, valueToSearch + 1);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .hasSize(1)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(valueToSearch))
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int valueToSearch = 42;
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, valueToSearch + 1);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int lowerBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, lowerBound + 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, lowerBound + 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .hasSize(1)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() > lowerBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int lowerBound = 42;
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound + 1);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int lowerBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, lowerBound + 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, lowerBound + 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() >= lowerBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int lowerBound = 42;
        QueryCloudTask task1 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound + 1);
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, lowerBound - 1);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int upperBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, upperBound - 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, upperBound - 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() < upperBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int upperBound = 42;
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound - 1);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "int-var";
        int upperBound = 42;

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.INTEGER, upperBound - 1);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.INTEGER, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.INTEGER, upperBound - 1);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() <= upperBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "int-var";
        int upperBound = 42;
        QueryCloudTask task1 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound - 1);
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound);
        createTaskWithVariable(processInstance, varName, VariableType.INTEGER, upperBound + 1);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "bigdecimal-var";
        BigDecimal valueToSearch = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, valueToSearch);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.BIGDECIMAL, valueToSearch);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .anyMatch(pv ->
                        pv.getName().equals(varName) && new BigDecimal(pv.getValue().toString()).equals(valueToSearch)
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal valueToSearch = new BigDecimal("42.42");
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isGreaterThan(lowerBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, lowerBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isGreaterThanOrEqualTo(lowerBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.43")
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, lowerBound);
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, new BigDecimal("42.41"));

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.41"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isLessThan(upperBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, upperBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BIGDECIMAL, new BigDecimal("42.41"));
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BIGDECIMAL, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isLessThanOrEqualTo(upperBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.BIGDECIMAL,
            new BigDecimal("42.41")
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, upperBound);
        createTaskWithVariable(processInstance, varName, VariableType.BIGDECIMAL, new BigDecimal("42.43"));

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String valueToSearch = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-03T00:12:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-02T00:13:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isEqualTo(DateUtils.parseDate(valueToSearch));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String valueToSearch = "2024-08-02";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-02T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-03T00:12:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String lowerBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-03T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-03T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isAfter(DateUtils.parseDate(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String lowerBound = "2024-08-02";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-03T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String lowerBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-03T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-03T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isAfterOrEqualTo(DateUtils.parseDate(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String lowerBound = "2024-08-02";
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-03T00:11:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-02T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-01T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String upperBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-01T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isBefore(DateUtils.parseDate(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String upperBound = "2024-08-02";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "date-var";
        String upperBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATE, "2024-08-01T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATE, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()).toLocalDate())
                            .isBeforeOrEqualTo(DateUtils.parseDate(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "date-var";
        String upperBound = "2024-08-02";
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-01T00:11:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATE,
            "2024-08-02T00:11:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATE, "2024-08-03T00:11:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateTimeProcessVariable_equals() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String valueToSearch = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:11:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isEqualTo(DateUtils.parseDateTime(valueToSearch));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_equals() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";

        String valueToSearch = "2024-08-02T00:11:00.000+0000";
        QueryCloudTask task = createTaskWithVariable(processInstance, varName, VariableType.DATETIME, valueToSearch);
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_greaterThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue())).isAfter(DateUtils.parseDateTime(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";
        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, lowerBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_greaterThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, lowerBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isAfterOrEqualTo(DateUtils.parseDateTime(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+0000";
        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.DATETIME, lowerBound);
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_lessThan() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isBefore(DateUtils.parseDateTime(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        QueryCloudTask task = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, upperBound);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task);
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_lessThanOrEqual() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+0000");
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.DATETIME, upperBound);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(
            processWithDifferentKey,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrderElementsOf(expectedTasks)
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(DateUtils.parseDateTime(pv.getValue()))
                            .isBeforeOrEqualTo(DateUtils.parseDateTime(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+0000";

        QueryCloudTask task1 = createTaskWithVariable(
            processInstance,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+0000"
        );
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.DATETIME, upperBound);
        createTaskWithVariable(processInstance, varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+0000");

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasksFilteredByBooleanProcessVariable() {
        String processDefinitionKey = "process-definition-key";
        String differentProcessDefinitionKey = "different-process-definition-key";

        String varName = "boolean-var";

        ProcessInstanceEntity processInstance1 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance1, varName, VariableType.BOOLEAN, true);
        ProcessInstanceEntity processInstance2 = createProcessInstance(processDefinitionKey);
        createProcessVariableAndTask(processInstance2, varName, VariableType.BOOLEAN, false);
        ProcessInstanceEntity processWithDifferentKey = createProcessInstance(differentProcessDefinitionKey);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.BOOLEAN, true);
        createProcessVariableAndTask(processWithDifferentKey, varName, VariableType.BOOLEAN, false);

        VariableFilter variableFilter = new VariableFilter(
            processDefinitionKey,
            varName,
            VariableType.BOOLEAN,
            String.valueOf(true),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance1.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat((boolean) pv.getValue()).isTrue();
                    })
            );

        variableFilter =
            new VariableFilter(
                processDefinitionKey,
                varName,
                VariableType.BOOLEAN,
                String.valueOf(false),
                FilterOperator.EQUALS
            );

        taskSearchRequest = buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

        retrievedTasks =
            taskControllerHelper
                .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(EntityModel::getContent)
                .toList();

        assertThat(retrievedTasks)
            .containsExactly(processInstance2.getTasks().iterator().next())
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat((boolean) pv.getValue()).isFalse();
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByBooleanTaskVariable() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String varName = "boolean-var";

        QueryCloudTask task1 = createTaskWithVariable(processInstance, varName, VariableType.BOOLEAN, true);
        QueryCloudTask task2 = createTaskWithVariable(processInstance, varName, VariableType.BOOLEAN, false);

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BOOLEAN,
            String.valueOf(true),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task1);

        variableFilter =
            new VariableFilter(null, varName, VariableType.BOOLEAN, String.valueOf(false), FilterOperator.EQUALS);

        taskSearchRequest = buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

        retrievedTasks =
            taskControllerHelper
                .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(EntityModel::getContent)
                .toList();

        assertThat(retrievedTasks).containsExactly(task2);
    }

    @Test
    void should_returnStandaloneTasksOnly() {
        TaskEntity standalone = new TaskEntity();
        String taskId = "standalone";
        standalone.setId(taskId);
        taskRepository.save(standalone);

        ProcessInstanceEntity processInstance = createProcessInstance();
        createTask(processInstance);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            true,
            false,
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

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(standalone);
    }

    @Test
    void should_returnRootTasksOnly() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        TaskEntity rootTask = createTask(processInstance);
        TaskEntity subTask = new TaskEntity();
        String subTaskId = "subTask";
        subTask.setId(subTaskId);
        subTask.setProcessInstanceId(processInstance.getId());
        subTask.setParentTaskId(rootTask.getId());
        taskRepository.save(subTask);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            true,
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

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(rootTask);
    }

    @Test
    void should_returnTasksFilteredByNameContains() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setName("Darth Vader");
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setName("Frodo Baggins");
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setName("Duke Leto");
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            List.of("darth", "baggins"),
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

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    private void createProcessVariableAndTask(
        ProcessInstanceEntity processInstance,
        String name,
        VariableType variableType,
        Object value
    ) {
        ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
        processVariableEntity.setName(name);
        processVariableEntity.setType(variableType.name().toLowerCase());
        switch (variableType) {
            case STRING, BIGDECIMAL, DATE, DATETIME -> processVariableEntity.setValue(String.valueOf(value));
            case INTEGER -> processVariableEntity.setValue((Integer) value);
            case BOOLEAN -> processVariableEntity.setValue((Boolean) value);
        }
        processVariableEntity.setProcessInstanceId(processInstance.getId());
        processVariableEntity.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
        variableRepository.save(processVariableEntity);
        processInstance.setVariables(Set.of(processVariableEntity));
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(UUID.randomUUID().toString());
        taskEntity.setCreatedDate(new Date());
        taskEntity.setProcessVariables(processInstance.getVariables());
        taskEntity.setProcessInstance(processInstance);
        taskEntity.setProcessInstanceId(processInstance.getId());
        taskRepository.save(taskEntity);
        processInstance.setTasks(Set.of(taskEntity));
        processInstanceRepository.save(processInstance);
    }

    @NotNull
    private static TaskSearchRequest buildTaskSearchRequestWithProcessVariableFilter(VariableFilter variableFilter) {
        Set<VariableFilter> filters = Set.of(variableFilter);
        Set<ProcessVariableKey> processVariableKeys = Set.of(
            new ProcessVariableKey(variableFilter.processDefinitionKey(), variableFilter.name())
        );
        return new TaskSearchRequest(
            false,
            false,
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
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            filters,
            processVariableKeys
        );
    }

    @NotNull
    private static TaskSearchRequest buildTaskSearchRequestWithTaskVariableFilter(VariableFilter variableFilter) {
        return new TaskSearchRequest(
            false,
            false,
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
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Set.of(variableFilter),
            null,
            null
        );
    }

    @NotNull
    private TaskEntity createTask(ProcessInstanceEntity processInstanceEntity) {
        TaskEntity taskEntity = new TaskEntity();
        String taskId = UUID.randomUUID().toString();
        taskEntity.setId(taskId);
        taskEntity.setCreatedDate(new Date());
        taskEntity.setProcessVariables(processInstanceEntity.getVariables());
        taskEntity.setProcessInstance(processInstanceEntity);
        taskEntity.setProcessInstanceId(processInstanceEntity.getId());
        taskRepository.save(taskEntity);
        processInstanceEntity.setTasks(Set.of(taskEntity));
        processInstanceRepository.save(processInstanceEntity);
        return taskEntity;
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance(String processDefinitionKey) {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId(UUID.randomUUID().toString());
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey(processDefinitionKey);
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance() {
        return createProcessInstance("processDefinitionKey");
    }

    private TaskEntity createTaskWithVariable(
        ProcessInstanceEntity processInstance,
        String varName,
        VariableType variableType,
        Object value
    ) {
        TaskEntity taskEntity = new TaskEntity();
        String taskId = UUID.randomUUID().toString();
        taskEntity.setId(taskId);
        taskEntity.setCreatedDate(new Date());
        taskEntity.setProcessInstanceId(processInstance.getId());

        TaskVariableEntity taskVariableEntity = new TaskVariableEntity();
        taskVariableEntity.setName(varName);
        taskVariableEntity.setType(variableType.name().toLowerCase());
        switch (variableType) {
            case STRING, BIGDECIMAL, DATE, DATETIME -> taskVariableEntity.setValue(String.valueOf(value));
            case INTEGER -> taskVariableEntity.setValue((Integer) value);
            case BOOLEAN -> taskVariableEntity.setValue((Boolean) value);
        }
        taskVariableEntity.setTaskId(taskId);
        taskVariableRepository.save(taskVariableEntity);
        taskEntity.setVariables(Set.of(taskVariableEntity));
        taskRepository.save(taskEntity);

        return taskEntity;
    }
}
