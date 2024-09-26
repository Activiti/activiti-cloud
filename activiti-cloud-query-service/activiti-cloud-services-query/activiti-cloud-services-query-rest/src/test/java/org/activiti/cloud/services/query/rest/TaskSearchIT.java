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
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.task.model.QueryCloudTask;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
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
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
)
@Testcontainers
@TestPropertySource("classpath:application-test.properties")
class TaskSearchIT {

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
    private QueryTestUtils queryTestUtils;

    @AfterEach
    public void cleanUp() {
        queryTestUtils.cleanUp();
    }

    @Test
    void should_returnTask_filteredByProcessVariable_whenAllFiltersMatch() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

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

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilters(
            Set.of(matchingFilter1, matchingFilter2)
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance.getTasks().stream().findFirst().get());
    }

    @Test
    void should_not_returnTask_filteredByProcessVariable_when_OneFilterDoesNotMatch() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

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
            "not-matching-value",
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilters(
            Set.of(matchingFilter, notMatchingFilter)
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).isEmpty();
    }

    @Test
    void should_returnTask_filteredByTaskVariable_whenAllFiltersMatch() {
        QueryTestUtils.VariableInput var1 = new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1");
        QueryTestUtils.VariableInput var2 = new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2");

        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withTasks(queryTestUtils.buildTask().withVariables(var1, var2))
            .buildAndSave();

        VariableFilter matchingFilter1 = new VariableFilter(
            null,
            var1.name(),
            VariableType.STRING,
            var1.getValue(),
            FilterOperator.EQUALS
        );

        VariableFilter matchingFilter2 = new VariableFilter(
            null,
            var2.name(),
            VariableType.STRING,
            var2.getValue(),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            matchingFilter1,
            matchingFilter2
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(processInstance.getTasks().stream().findFirst().get());
    }

    @Test
    void should_not_returnTask_filteredByTaskVariable_when_OneFilterDoesNotMatch() {
        QueryTestUtils.VariableInput var1 = new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1");
        QueryTestUtils.VariableInput var2 = new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2");

        queryTestUtils
            .buildProcessInstance()
            .withTasks(queryTestUtils.buildTask().withVariables(var1, var2))
            .buildAndSave();

        VariableFilter matchingFilter1 = new VariableFilter(
            null,
            var1.name(),
            VariableType.STRING,
            var1.getValue(),
            FilterOperator.EQUALS
        );

        VariableFilter notMatchingFilter = new VariableFilter(
            null,
            var2.name(),
            VariableType.STRING,
            "not-matching-value",
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            matchingFilter1,
            notMatchingFilter
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).isEmpty();
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_exactMatch() {
        QueryTestUtils.VariableInput varToSearch = new QueryTestUtils.VariableInput(
            "string-var",
            VariableType.STRING,
            "string-value"
        );
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("process-definition-key")
            .withVariables(varToSearch)
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("other-process-definition-key")
            .withVariables(
                new QueryTestUtils.VariableInput(varToSearch.name(), VariableType.STRING, "different-string-value")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varToSearch.name(),
            VariableType.STRING,
            varToSearch.getValue(),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv -> pv.getName().equals(varToSearch.name()) && pv.getValue().equals(varToSearch.value())
                    );
            });
    }

    @Test
    void should_returnTasks_filteredByStringTaskProcessVariable_exactMatch() {
        QueryTestUtils.VariableInput varToSearch = new QueryTestUtils.VariableInput(
            "string-var",
            VariableType.STRING,
            "string-value"
        );
        queryTestUtils
            .buildProcessInstance()
            .withTasks(
                queryTestUtils.buildTask().withVariables(varToSearch),
                queryTestUtils
                    .buildTask()
                    .withVariables(
                        new QueryTestUtils.VariableInput(varToSearch.name(), VariableType.STRING, "other-value")
                    )
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varToSearch.name(),
            VariableType.STRING,
            varToSearch.getValue(),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .hasSize(1)
            .asInstanceOf(InstanceOfAssertFactories.list(TaskEntity.class))
            .satisfiesExactly(task ->
                assertThat(task.getVariable(varToSearch.name()))
                    .hasValueSatisfying(variable ->
                        assertThat((String) variable.getValue()).isEqualTo(varToSearch.value())
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_contains() {
        String varName = "string-var";
        String valueToSearch = "jaeger";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.STRING, "Eren Jaeger"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.STRING, "Frank Jaeger"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.STRING, "Jaeger"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.LIKE
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv ->
                        pv.getName().equals(varName) &&
                        ((String) pv.getValue()).toLowerCase().contains(valueToSearch.toLowerCase())
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByTaskProcessVariable_contains() {
        String varName = "task-var";
        String valueToSearch = "fox";
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withTasks(
                queryTestUtils
                    .buildTask()
                    .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.STRING, "Gray Fox")),
                queryTestUtils
                    .buildTask()
                    .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.STRING, "Fox Hound"))
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.STRING,
            valueToSearch,
            FilterOperator.LIKE
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(processInstance.getTasks());
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_equals() {
        String varName = "int-var";
        int valueToSearch = 42;

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("process-definition-key")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, valueToSearch))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, valueToSearch + 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("different-process-definition-key")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, valueToSearch))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.INTEGER,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv -> pv.getName().equals(varName) && pv.getValue().equals(valueToSearch))
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_equals() {
        String varName = "int-var";
        int valueToSearch = 42;
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, valueToSearch))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, valueToSearch + 1))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "int-var";
        int lowerBound = 42;

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound + 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound + 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() > lowerBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_greaterThan() {
        String varName = "int-var";
        int lowerBound = 42;
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound + 1))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound));

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "int-var";
        int lowerBound = 42;

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound + 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound + 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() >= lowerBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "int-var";
        int lowerBound = 42;
        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound + 1))
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, lowerBound - 1))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "int-var";
        int upperBound = 42;

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound - 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound - 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() < upperBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "int-var";
        int upperBound = 42;
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound - 1))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "int-var";
        int upperBound = 42;

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound - 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound - 1))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv -> pv.getName().equals(varName) && (int) pv.getValue() <= upperBound)
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "int-var";
        int upperBound = 42;
        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound - 1))
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, upperBound + 1))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "bigdecimal-var";
        BigDecimal valueToSearch = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, valueToSearch))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, valueToSearch))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .anyMatch(pv ->
                        pv.getName().equals(varName) && new BigDecimal(pv.getValue().toString()).equals(valueToSearch)
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_equals() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "bigdecimal-var";
        BigDecimal valueToSearch = new BigDecimal("42.42");
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, valueToSearch))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(valueToSearch),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, lowerBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isGreaterThan(lowerBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, lowerBound))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, lowerBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isGreaterThanOrEqualTo(lowerBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "bigdecimal-var";
        BigDecimal lowerBound = new BigDecimal("42.42");
        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, lowerBound))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.41")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(lowerBound),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.41")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, upperBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.41")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isLessThan(upperBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.41")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, upperBound))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.41")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, upperBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.41")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(new BigDecimal(pv.getValue().toString())).isLessThanOrEqualTo(upperBound);
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "bigdecimal-var";
        BigDecimal upperBound = new BigDecimal("42.42");
        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.41")))
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, upperBound))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42.43")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            String.valueOf(upperBound),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "date-var";
        String valueToSearch = "2024-08-02";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATE,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(LocalDate.parse(pv.getValue())).isEqualTo(LocalDate.parse(valueToSearch));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_equals() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "date-var";
        String valueToSearch = "2024-08-02";
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "date-var";
        String lowerBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(LocalDate.parse(pv.getValue())).isGreaterThan(LocalDate.parse(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "date-var";
        String lowerBound = "2024-08-02";
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "date-var";
        String lowerBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(LocalDate.parse(pv.getValue())).isGreaterThanOrEqualTo(LocalDate.parse(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "date-var";
        String lowerBound = "2024-08-02";
        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "date-var";
        String upperBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(LocalDate.parse(pv.getValue())).isLessThan(LocalDate.parse(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "date-var";
        String upperBound = "2024-08-02";
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "date-var";
        String upperBound = "2024-08-02";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(LocalDate.parse(pv.getValue())).isLessThanOrEqualTo(LocalDate.parse(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "date-var";
        String upperBound = "2024-08-02";
        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-03"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "datetime-var";
        String valueToSearch = "2024-08-02T00:11:00.000+00:00";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATETIME,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(OffsetDateTime.parse(pv.getValue())).isEqualTo(OffsetDateTime.parse(valueToSearch));
                    })
            );
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_equals() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "datetime-var";

        String valueToSearch = "2024-08-02T00:11:00.000+00:00";
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, valueToSearch))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            valueToSearch,
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+00:00";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, lowerBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(OffsetDateTime.parse(pv.getValue())).isAfter(OffsetDateTime.parse(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_greaterThan() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+00:00";
        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, lowerBound))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+00:00";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, lowerBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(OffsetDateTime.parse(pv.getValue()))
                            .isAfterOrEqualTo(OffsetDateTime.parse(lowerBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_greaterThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "datetime-var";
        String lowerBound = "2024-08-02T00:11:00.000+00:00";
        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, lowerBound))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            lowerBound,
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+00:00";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, upperBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(OffsetDateTime.parse(pv.getValue())).isBefore(OffsetDateTime.parse(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lessThan() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+00:00";

        QueryCloudTask task = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, upperBound))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+00:00";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, upperBound))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat(OffsetDateTime.parse(pv.getValue()))
                            .isBeforeOrEqualTo(OffsetDateTime.parse(upperBound));
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lessThanOrEqual() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "datetime-var";
        String upperBound = "2024-08-02T00:11:00.000+00:00";

        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, upperBound))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            upperBound,
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

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
        String varName = "boolean-var";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("processDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BOOLEAN, true))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BOOLEAN, false))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey("differentProcessDefinitionKey")
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BOOLEAN, true))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BOOLEAN,
            String.valueOf(true),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(
            variableFilter
        );

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat((boolean) pv.getValue()).isTrue();
                    })
            );

        variableFilter =
            new VariableFilter(
                processInstance1.getProcessDefinitionKey(),
                varName,
                VariableType.BOOLEAN,
                String.valueOf(false),
                FilterOperator.EQUALS
            );

        taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithProcessVariableFilter(variableFilter);

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
                    .isNotEmpty()
                    .allSatisfy(pv -> {
                        assertThat(pv.getName()).isEqualTo(varName);
                        assertThat((boolean) pv.getValue()).isFalse();
                    })
            );
    }

    @Test
    void should_returnTasksFilteredByBooleanTaskVariable() {
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        String varName = "boolean-var";

        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BOOLEAN, true))
            .buildAndSave();
        QueryCloudTask task2 = queryTestUtils
            .buildTask()
            .withParentProcess(processInstance)
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BOOLEAN, false))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varName,
            VariableType.BOOLEAN,
            String.valueOf(true),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(
            variableFilter
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasks(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task1);

        variableFilter =
            new VariableFilter(null, varName, VariableType.BOOLEAN, String.valueOf(false), FilterOperator.EQUALS);

        taskSearchRequest = QueryTestUtils.buildTaskSearchRequestWithTaskVariableFilter(variableFilter);

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

        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        queryTestUtils.buildTask().withParentProcess(processInstance).buildAndSave();

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
        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        TaskEntity rootTask = queryTestUtils.buildTask().withParentProcess(processInstance).buildAndSave();
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
            Set.of("darth", "baggins"),
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

    @Test
    void should_returnTasksFilteredByDescriptionContains() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setDescription("Darth Vader");
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setDescription("Frodo Baggins");
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setDescription("Duke Leto");
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            null,
            Set.of("darth", "baggins"),
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

    @Test
    void should_returnTasksFilteredByPriority() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setPriority(1);
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setPriority(2);
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setPriority(3);
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            null,
            null,
            Set.of(1, 2),
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

    @Test
    void should_returnTasksFilteredByStatus() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setStatus(Task.TaskStatus.CREATED);
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setStatus(Task.TaskStatus.ASSIGNED);
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setStatus(Task.TaskStatus.COMPLETED);
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            Set.of(Task.TaskStatus.CREATED, Task.TaskStatus.ASSIGNED),
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

    @Test
    void should_returnTasksFilteredByCompletedBy() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setCompletedBy("Jimmy Page");
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setCompletedBy("Robert Plant");
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setCompletedBy("John Bonham");
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            Set.of("Jimmy Page", "Robert Plant"),
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

    @Test
    void should_returnTasksFilteredByAssignee() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setAssignee("Kimi Raikkonen");
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setAssignee("Lewis Hamilton");
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setAssignee("Max Verstappen");
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            Set.of("Kimi Raikkonen", "Lewis Hamilton"),
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

    @Test
    void should_returnTasksFilteredByCreatedFrom() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setCreatedDate(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setCreatedDate(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setCreatedDate(new Date(500));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            new Date(900),
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

    @Test
    void should_returnTasksFilteredByCreatedTo() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setCreatedDate(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setCreatedDate(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setCreatedDate(new Date(3000));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new Date(2500),
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

    @Test
    void should_returnTasksFilteredByLastModifiedFrom() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setLastModified(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setLastModified(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setLastModified(new Date(500));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(900),
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

    @Test
    void should_returnTasksFilteredByLastModifiedTo() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setLastModified(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setLastModified(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setLastModified(new Date(3000));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(2500),
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

    @Test
    void should_returnTasksFilteredByLastClaimedFrom() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setClaimedDate(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setClaimedDate(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setClaimedDate(new Date(500));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(900),
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

    @Test
    void should_returnTasksFilteredByLastClaimedTo() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setClaimedDate(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setClaimedDate(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setClaimedDate(new Date(3000));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(2500),
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

    @Test
    void should_returnTasksFilteredByDueDateFrom() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setDueDate(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setDueDate(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setDueDate(new Date(500));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(900),
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

    @Test
    void should_returnTasksFilteredByDueDateTo() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setDueDate(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setDueDate(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setDueDate(new Date(3000));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(2500),
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

    @Test
    void should_returnTasksFilteredByCompletedFrom() {
        TaskEntity task1 = queryTestUtils.buildTask().withCompletedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withCompletedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCompletedDate(new Date(500)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(900),
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

    @Test
    void should_returnTasksFilteredByCompletedTo() {
        TaskEntity task1 = new TaskEntity();
        task1.setId("task1");
        task1.setCompletedDate(new Date(1000));
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setId("task2");
        task2.setCompletedDate(new Date(2000));
        taskRepository.save(task2);

        TaskEntity task3 = new TaskEntity();
        task3.setId("task3");
        task3.setCompletedDate(new Date(3000));
        taskRepository.save(task3);

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            new Date(2500),
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

    @Test
    void should_returnTasksFilteredByCandidateUserId() {
        TaskEntity task1 = queryTestUtils.buildTask().withTaskCandidateUsers("user1").buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withTaskCandidateUsers("user2").buildAndSave();
        queryTestUtils.buildTask().withTaskCandidateUsers("user3").buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            Set.of("user1", "user2"),
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

    @Test
    void should_returnTasksFilteredByCandidateGroupId() {
        TaskEntity task1 = queryTestUtils.buildTask().withTaskCandidateGroups("group1").buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withTaskCandidateGroups("group2").buildAndSave();
        queryTestUtils.buildTask().withTaskCandidateGroups("group3").buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequest(
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
            Set.of("group1", "group2"),
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
}
