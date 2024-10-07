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
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.TaskSearchRequest;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
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
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(matchingFilter1, matchingFilter2))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(matchingFilter, notMatchingFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(matchingFilter1, matchingFilter2))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(matchingFilter1, notMatchingFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskEntity task1 = queryTestUtils.buildTask().withVariables(varToSearch).buildAndSave();
        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varToSearch.name(), VariableType.STRING, "other-value"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            varToSearch.name(),
            VariableType.STRING,
            varToSearch.getValue(),
            FilterOperator.EQUALS
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task1);
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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
        queryTestUtils
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_range() {
        String varName = "int-var";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 84))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 10))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 100))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.INTEGER,
            String.valueOf(20),
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLtEq = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.INTEGER,
            String.valueOf(84),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilterGt, variableFilterLtEq))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrder(
                processInstance1.getTasks().iterator().next(),
                processInstance2.getTasks().iterator().next()
            )
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .isNotEmpty()
                    .anyMatch(pv ->
                        pv.getName().equals(varName) && (int) pv.getValue() > 20 && (int) pv.getValue() <= 84
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_range() {
        String varName = "int-var";

        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 42))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 84))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 10))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.INTEGER, 100))
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(20),
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLtEq = new VariableFilter(
            null,
            varName,
            VariableType.INTEGER,
            String.valueOf(84),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilterGt, variableFilterLtEq))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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
        queryTestUtils
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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
        queryTestUtils
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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
        queryTestUtils
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_range() {
        String varName = "bigdecimal-var";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("84")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("10")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("100")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BIGDECIMAL,
            "20",
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLtEq = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.BIGDECIMAL,
            "84",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilterGt, variableFilterLtEq))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                processInstance1.getTasks().iterator().next(),
                processInstance2.getTasks().iterator().next()
            )
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .isNotEmpty()
                    .anyMatch(pv ->
                        pv.getName().equals(varName) &&
                        new BigDecimal(pv.getValue().toString()).compareTo(new BigDecimal("20")) > 0 &&
                        (
                            new BigDecimal(pv.getValue().toString()).compareTo(new BigDecimal("84")) < 0 ||
                            new BigDecimal(pv.getValue().toString()).compareTo(new BigDecimal("84")) == 0
                        )
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_range() {
        String varName = "bigdecimal-var";

        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("42")))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("84")))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("10")))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.BIGDECIMAL, new BigDecimal("100")))
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            "20",
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLt = new VariableFilter(
            null,
            varName,
            VariableType.BIGDECIMAL,
            "84",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilterGt, variableFilterLt))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_range() {
        String varName = "date-var";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-04"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-05"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATE,
            "2024-08-01",
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLt = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATE,
            "2024-08-04",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilterGt, variableFilterLt))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrder(
                processInstance1.getTasks().iterator().next(),
                processInstance2.getTasks().iterator().next()
            )
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .isNotEmpty()
                    .anyMatch(pv ->
                        pv.getName().equals(varName) &&
                        LocalDate.parse(pv.getValue()).isAfter(LocalDate.parse("2024-08-01")) &&
                        (
                            LocalDate.parse(pv.getValue()).isBefore(LocalDate.parse("2024-08-04")) ||
                            LocalDate.parse(pv.getValue()).isEqual(LocalDate.parse("2024-08-04"))
                        )
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_range() {
        String varName = "date-var";

        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-04"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-01"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(varName, VariableType.DATE, "2024-08-05"))
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            "2024-08-01",
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLt = new VariableFilter(
            null,
            varName,
            VariableType.DATE,
            "2024-08-04",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilterGt, variableFilterLt))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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
        queryTestUtils
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(
            processInstance1.getTasks().iterator().next(),
            processInstance2.getTasks().iterator().next()
        );

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1, task2);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrderElementsOf(expectedTasks);
    }

    @Test
    void should_returnTasks_filteredByDateTimeProcessVariable_range() {
        String varName = "datetime-var";

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(processInstance1.getProcessDefinitionKey())
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:14:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+00:00",
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLt = new VariableFilter(
            processInstance1.getProcessDefinitionKey(),
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:13:00.000+00:00",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilterGt, variableFilterLt))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks)
            .containsExactlyInAnyOrder(
                processInstance1.getTasks().iterator().next(),
                processInstance2.getTasks().iterator().next()
            )
            .allSatisfy(task ->
                assertThat(task.getProcessVariables())
                    .isNotEmpty()
                    .anyMatch(pv ->
                        pv.getName().equals(varName) &&
                        OffsetDateTime
                            .parse(pv.getValue())
                            .isAfter(OffsetDateTime.parse("2024-08-02T00:10:00.000+00:00")) &&
                        OffsetDateTime
                            .parse(pv.getValue())
                            .isBefore(OffsetDateTime.parse("2024-08-02T00:13:00.000+00:00"))
                    )
            );
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_range() {
        String varName = "datetime-var";

        QueryCloudTask task1 = queryTestUtils
            .buildTask()
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:10:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(
                new QueryTestUtils.VariableInput(varName, VariableType.DATETIME, "2024-08-02T00:14:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilterGt = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:10:00.000+00:00",
            FilterOperator.GREATER_THAN
        );

        VariableFilter variableFilterLt = new VariableFilter(
            null,
            varName,
            VariableType.DATETIME,
            "2024-08-02T00:13:00.000+00:00",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilterGt, variableFilterLt))
            .build();

        List<QueryCloudTask> expectedTasks = List.of(task1);

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        taskSearchRequest = new TaskSearchRequestBuilder().withProcessVariableFilters(Set.of(variableFilter)).build();

        retrievedTasks =
            taskControllerHelper
                .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(Set.of(variableFilter))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(task1);

        variableFilter =
            new VariableFilter(null, varName, VariableType.BOOLEAN, String.valueOf(false), FilterOperator.EQUALS);

        taskSearchRequest = new TaskSearchRequestBuilder().withTaskVariableFilters(Set.of(variableFilter)).build();

        retrievedTasks =
            taskControllerHelper
                .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(EntityModel::getContent)
                .toList();

        assertThat(retrievedTasks).containsExactly(task2);
    }

    @Test
    void should_returnStandaloneTasksOnly() {
        TaskEntity standalone = queryTestUtils.buildTask().buildAndSave();

        ProcessInstanceEntity processInstance = queryTestUtils.buildProcessInstance().buildAndSave();
        queryTestUtils.buildTask().withParentProcess(processInstance).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().onlyStandalone().build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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
        queryTestUtils.buildTask().withParentTask(rootTask).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().onlyRoot().build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactly(rootTask);
    }

    @Test
    void should_returnTasksFilteredByNameContains() {
        TaskEntity task1 = queryTestUtils.buildTask().withName("Darth Vader").buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withName("Frodo Baggins").buildAndSave();
        queryTestUtils.buildTask().withName("Duke Leto").buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withName("darth", "baggins").build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByDescriptionContains() {
        TaskEntity task1 = queryTestUtils.buildTask().withDescription("Darth Vader").buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withDescription("Frodo Baggins").buildAndSave();
        queryTestUtils.buildTask().withDescription("Duke Leto").buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withDescription("darth", "baggins")
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByProcessDefinitionName() {
        TaskEntity task1 = queryTestUtils.buildTask().withProcessDefinitionName("name1").buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withProcessDefinitionName("name2").buildAndSave();
        queryTestUtils.buildTask().withProcessDefinitionName("name3").buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withProcessDefinitionName("name1", "name2")
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByPriority() {
        TaskEntity task1 = queryTestUtils.buildTask().withPriority(1).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withPriority(2).buildAndSave();
        queryTestUtils.buildTask().withPriority(3).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withPriority(1, 2).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByStatus() {
        TaskEntity task1 = queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withStatus(Task.TaskStatus.ASSIGNED).buildAndSave();
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.COMPLETED).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withStatus(Task.TaskStatus.CREATED, Task.TaskStatus.ASSIGNED)
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByCompletedBy() {
        TaskEntity task1 = queryTestUtils.buildTask().withCompletedBy("Jimmy Page").buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withCompletedBy("Robert Plant").buildAndSave();
        queryTestUtils.buildTask().withCompletedBy("John Bonham").buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withCompletedBy("Jimmy Page", "Robert Plant")
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByAssignee() {
        TaskEntity task1 = queryTestUtils.buildTask().withAssignee("Kimi Raikkonen").buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withAssignee("Lewis Hamilton").buildAndSave();
        queryTestUtils.buildTask().withAssignee("Sebastian Vettel").buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withAssignees("Kimi Raikkonen", "Lewis Hamilton")
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByCreatedFrom() {
        TaskEntity task1 = queryTestUtils.buildTask().withCreatedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withCreatedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCreatedDate(new Date(500)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withCreatedFrom(new Date(900)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByCreatedTo() {
        TaskEntity task1 = queryTestUtils.buildTask().withCreatedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withCreatedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCreatedDate(new Date(3000)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withCreatedTo(new Date(2500)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByLastModifiedFrom() {
        TaskEntity task1 = queryTestUtils.buildTask().withLastModifiedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withLastModifiedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withLastModifiedDate(new Date(500)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withLastModifiedFrom(new Date(900))
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByLastModifiedTo() {
        TaskEntity task1 = queryTestUtils.buildTask().withLastModifiedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withLastModifiedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withLastModifiedDate(new Date(3000)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withLastModifiedTo(new Date(2500)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByLastClaimedFrom() {
        TaskEntity task1 = queryTestUtils.buildTask().withClaimedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withClaimedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withClaimedDate(new Date(500)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withLastClaimedFrom(new Date(900)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByLastClaimedTo() {
        TaskEntity task1 = queryTestUtils.buildTask().withClaimedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withClaimedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withClaimedDate(new Date(3000)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withLastClaimedTo(new Date(2500)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByDueDateFrom() {
        TaskEntity task1 = queryTestUtils.buildTask().withDueDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withDueDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withDueDate(new Date(500)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withDueDateFrom(new Date(900)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByDueDateTo() {
        TaskEntity task1 = queryTestUtils.buildTask().withDueDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withDueDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withDueDate(new Date(3000)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withDueDateTo(new Date(2500)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withCompletedFrom(new Date(900)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }

    @Test
    void should_returnTasksFilteredByCompletedTo() {
        TaskEntity task1 = queryTestUtils.buildTask().withCompletedDate(new Date(1000)).buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().withCompletedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCompletedDate(new Date(3000)).buildAndSave();

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder().withCompletedTo(new Date(2500)).build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withCandidateUserId("user1", "user2")
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
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

        TaskSearchRequest taskSearchRequest = new TaskSearchRequestBuilder()
            .withCandidateGroupId("group1", "group2")
            .build();

        List<QueryCloudTask> retrievedTasks = taskControllerHelper
            .searchTasksUnrestricted(taskSearchRequest, PageRequest.of(0, 100))
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .toList();

        assertThat(retrievedTasks).containsExactlyInAnyOrder(task1, task2);
    }
}
