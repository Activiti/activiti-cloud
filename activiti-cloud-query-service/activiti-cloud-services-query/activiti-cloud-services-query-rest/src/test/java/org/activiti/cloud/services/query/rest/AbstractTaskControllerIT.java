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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.postProcessors;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

public abstract class AbstractTaskControllerIT {

    protected static final String CURRENT_USER = "testuser";
    protected static final String VAR_NAME = "var-name";
    protected static final String PROCESS_DEFINITION_KEY = "process-definition-key";
    protected static final String TASK_ID_1 = "taskId1";
    protected static final String TASK_ID_2 = "taskId2";
    protected static final String TASK_ID_3 = "taskId3";
    protected static final String TASKS_JSON_PATH = "_embedded.tasks";
    protected static final String TASK_IDS_JSON_PATH = "_embedded.tasks.id";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected QueryTestUtils queryTestUtils;

    protected abstract String getSearchEndpointHttpGet();

    protected abstract String getSearchEndpointHttpPost();

    @BeforeEach
    public void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
        queryTestUtils.cleanUp();
    }

    @AfterEach
    public void cleanUp() {
        queryTestUtils.cleanUp();
    }

    @Test
    void should_returnTasks_withOnlyRequestedProcessVariables_whenSearchingByTaskVariableWithGetEndpoint() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput(UUID.randomUUID().toString(), VariableType.STRING, "value2")
            )
            .withTasks(
                queryTestUtils
                    .buildTask()
                    .withVariables(
                        new QueryTestUtils.VariableInput("taskVar1", VariableType.STRING, "taskValue1"),
                        new QueryTestUtils.VariableInput("taskVar2", VariableType.STRING, "taskValue2")
                    )
            )
            .buildAndSave();

        given()
            .param("variableKeys", PROCESS_DEFINITION_KEY + "/" + VAR_NAME)
            .param("variables.name", "taskVar1")
            .param("variables.value", "taskValue1")
            .when()
            .get(getSearchEndpointHttpGet())
            .then()
            .statusCode(200)
            .body("_embedded.tasks", hasSize(1))
            .body("_embedded.tasks[0].processVariables", hasSize(1))
            .body("_embedded.tasks[0].processVariables[0].name", is(VAR_NAME));
    }

    @Test
    void should_returnTasks_withOnlyRequestedProcessVariables_whenSearchingWithPostEndpoint() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput(UUID.randomUUID().toString(), VariableType.STRING, "value2")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.STRING,
                    "value1",
                    FilterOperator.EQUALS
                )
            )
            .withProcessVariableKeys(new ProcessVariableKey(PROCESS_DEFINITION_KEY, VAR_NAME));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body("_embedded.tasks", hasSize(1))
            .body("_embedded.tasks[0].processVariables", hasSize(1))
            .body("_embedded.tasks[0].processVariables[0].name", is(VAR_NAME));
    }

    @Test
    void should_returnTask_filteredByProcessVariable_whenAllFiltersMatch() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        VariableFilter matchingFilter1 = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        VariableFilter matchingFilter2 = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var2",
            VariableType.STRING,
            "value2",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(matchingFilter1, matchingFilter2);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_not_returnTask_filteredByProcessVariable_when_OneFilterDoesNotMatch() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter matchingFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var1",
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        VariableFilter notMatchingFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            "var2",
            VariableType.STRING,
            "not-matching-value",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(matchingFilter, notMatchingFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(0));
    }

    @Test
    void should_returnTask_filteredByTaskVariable_whenAllFiltersMatch() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .buildAndSave();

        VariableFilter matchingFilter1 = new VariableFilter(
            null,
            "var1",
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        VariableFilter matchingFilter2 = new VariableFilter(
            null,
            "var2",
            VariableType.STRING,
            "value2",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(matchingFilter1, matchingFilter2);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_not_returnTask_filteredByTaskVariable_when_OneFilterDoesNotMatch() {
        queryTestUtils
            .buildProcessInstance()
            .withTasks(
                queryTestUtils
                    .buildTask()
                    .withVariables(
                        new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                        new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
                    )
            )
            .buildAndSave();

        VariableFilter matchingFilter1 = new VariableFilter(
            null,
            "var1",
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        VariableFilter notMatchingFilter = new VariableFilter(
            null,
            "var2",
            VariableType.STRING,
            "not-matching-value",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(matchingFilter1, notMatchingFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(0));
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_exactMatch() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "string-value"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "different-value"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.STRING,
            "string-value",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByStringTaskVariable_exactMatch() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "string-value"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.STRING,
            "string-value",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_containsInAnyOrder() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "Eren Jaeger"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "Frank Jaeger"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.STRING,
            "jaeger",
            FilterOperator.LIKE
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByTaskProcessVariable_containsInAnyOrder() {
        queryTestUtils
            .buildProcessInstance()
            .withTasks(
                queryTestUtils
                    .buildTask()
                    .withId(TASK_ID_1)
                    .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "Gray Fox")),
                queryTestUtils
                    .buildTask()
                    .withId(TASK_ID_2)
                    .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "Fox Hound")),
                queryTestUtils
                    .buildTask()
                    .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "Jimmy Page"))
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.STRING,
            "fox",
            FilterOperator.LIKE
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(42),
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(42),
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(42),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withProcessVariableFilters(
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.INTEGER,
                String.valueOf(42),
                FilterOperator.GREATER_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(
                new VariableFilter(
                    null,
                    VAR_NAME,
                    VariableType.INTEGER,
                    String.valueOf(42),
                    FilterOperator.GREATER_THAN
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withTaskVariableFilters(
            new VariableFilter(
                null,
                VAR_NAME,
                VariableType.INTEGER,
                String.valueOf(42),
                FilterOperator.GREATER_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 41))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 41))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(42),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withProcessVariableFilters(
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.INTEGER,
                String.valueOf(42),
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 41))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(
                new VariableFilter(null, VAR_NAME, VariableType.INTEGER, String.valueOf(42), FilterOperator.LESS_THAN)
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withTaskVariableFilters(
            new VariableFilter(
                null,
                VAR_NAME,
                VariableType.INTEGER,
                String.valueOf(42),
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 84))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.INTEGER,
                    String.valueOf(42),
                    FilterOperator.GREATER_THAN_OR_EQUAL
                ),
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.INTEGER,
                    String.valueOf(84),
                    FilterOperator.LESS_THAN
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withProcessVariableFilters(
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.INTEGER,
                String.valueOf(42),
                FilterOperator.GREATER_THAN
            ),
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.INTEGER,
                String.valueOf(84),
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 84))
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(
                new VariableFilter(
                    null,
                    VAR_NAME,
                    VariableType.INTEGER,
                    String.valueOf(42),
                    FilterOperator.GREATER_THAN_OR_EQUAL
                ),
                new VariableFilter(null, VAR_NAME, VariableType.INTEGER, String.valueOf(84), FilterOperator.LESS_THAN)
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withTaskVariableFilters(
            new VariableFilter(null, VAR_NAME, VariableType.INTEGER, String.valueOf(42), FilterOperator.GREATER_THAN),
            new VariableFilter(
                null,
                VAR_NAME,
                VariableType.INTEGER,
                String.valueOf(84),
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.42")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("316.2")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.42")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("42.42")),
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.42")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("285.432"))
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("42.42")),
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("14.3")),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterGt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterGte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("14.3")),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withProcessVariableFilters(filterGte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("14.3")),
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(filterGt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterGte = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("14.3")),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withTaskVariableFilters(filterGte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("15.2")),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterLte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("15.2")),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withProcessVariableFilters(filterLte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .buildAndSave();

        VariableFilter filterLt = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("15.2")),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterLte = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("15.2")),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withTaskVariableFilters(filterLte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.1")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("84.2")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        VariableFilter filterGte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("42.1")),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("84.2")),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterGte, filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withProcessVariableFilters(
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.BIGDECIMAL,
                String.valueOf(new BigDecimal("42.1")),
                FilterOperator.GREATER_THAN
            ),
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.BIGDECIMAL,
                String.valueOf(new BigDecimal("84.2")),
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.1")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("84.2")))
            .buildAndSave();

        VariableFilter filterGte = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("42.1")),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        VariableFilter filterLt = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            String.valueOf(new BigDecimal("84.2")),
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(filterGte, filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withTaskVariableFilters(
            new VariableFilter(
                null,
                VAR_NAME,
                VariableType.BIGDECIMAL,
                String.valueOf(new BigDecimal("42.1")),
                FilterOperator.GREATER_THAN
            ),
            new VariableFilter(
                null,
                VAR_NAME,
                VariableType.BIGDECIMAL,
                String.valueOf(new BigDecimal("84.2")),
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-02",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-02",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-02",
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterGt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterGte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-02",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withProcessVariableFilters(filterGte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-02",
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(filterGt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterGte = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-02",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withTaskVariableFilters(filterGte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-03",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterLte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-03",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withProcessVariableFilters(filterLte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .buildAndSave();

        VariableFilter filterLt = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-03",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterLte = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-03",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withTaskVariableFilters(filterLte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-04"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        VariableFilter filterGte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-02",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-08-04",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterGte, filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withProcessVariableFilters(
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.DATE,
                "2024-08-02",
                FilterOperator.GREATER_THAN
            ),
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.DATE,
                "2024-08-04",
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-04"))
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(
                new VariableFilter(
                    null,
                    VAR_NAME,
                    VariableType.DATE,
                    "2024-08-02",
                    FilterOperator.GREATER_THAN_OR_EQUAL
                ),
                new VariableFilter(null, VAR_NAME, VariableType.DATE, "2024-08-04", FilterOperator.LESS_THAN)
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withTaskVariableFilters(
            new VariableFilter(null, VAR_NAME, VariableType.DATE, "2024-08-02", FilterOperator.GREATER_THAN),
            new VariableFilter(null, VAR_NAME, VariableType.DATE, "2024-08-04", FilterOperator.LESS_THAN_OR_EQUAL)
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateTimeProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+00:00",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+00:00",
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterGt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterGte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withProcessVariableFilters(filterGte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(filterGt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterGte = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withTaskVariableFilters(filterGte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+00:00",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterLte = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+00:00",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withProcessVariableFilters(filterLte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter filterLt = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+00:00",
            FilterOperator.LESS_THAN
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        VariableFilter filterLte = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-08-02T00:12:00.000+00:00",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        taskSearchRequestBuilder.withTaskVariableFilters(filterLte);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateTimeProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:14:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.DATETIME,
                    "2024-08-02T00:11:00.000+00:00",
                    FilterOperator.GREATER_THAN_OR_EQUAL
                ),
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.DATETIME,
                    "2024-08-02T00:14:00.000+00:00",
                    FilterOperator.LESS_THAN
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withProcessVariableFilters(
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.DATETIME,
                "2024-08-02T00:11:00.000+00:00",
                FilterOperator.GREATER_THAN
            ),
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.DATETIME,
                "2024-08-02T00:14:00.000+00",
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:14:00.000+00:00")
            )
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(
                new VariableFilter(
                    null,
                    VAR_NAME,
                    VariableType.DATETIME,
                    "2024-08-02T00:10:00.000+00:00",
                    FilterOperator.GREATER_THAN_OR_EQUAL
                ),
                new VariableFilter(
                    null,
                    VAR_NAME,
                    VariableType.DATETIME,
                    "2024-08-02T00:14:00.000+00:00",
                    FilterOperator.LESS_THAN
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        taskSearchRequestBuilder.withTaskVariableFilters(
            new VariableFilter(
                null,
                VAR_NAME,
                VariableType.DATETIME,
                "2024-08-02T00:11:00.000+00:00",
                FilterOperator.GREATER_THAN
            ),
            new VariableFilter(
                null,
                VAR_NAME,
                VariableType.DATETIME,
                "2024-08-02T00:14:00.000+00:00",
                FilterOperator.LESS_THAN_OR_EQUAL
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByBooleanProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, false))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(UUID.randomUUID().toString())
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BOOLEAN,
            String.valueOf(true),
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        variableFilter =
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.BOOLEAN,
                String.valueOf(false),
                FilterOperator.EQUALS
            );

        taskSearchRequestBuilder = new TaskSearchRequestBuilder().withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByBooleanTaskVariable() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, false))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            null,
            VAR_NAME,
            VariableType.BOOLEAN,
            String.valueOf(true),
            FilterOperator.EQUALS
        );

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        variableFilter =
            new VariableFilter(null, VAR_NAME, VariableType.BOOLEAN, String.valueOf(false), FilterOperator.EQUALS);

        taskSearchRequestBuilder = new TaskSearchRequestBuilder().withTaskVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2));
    }

    @Test
    void should_returnStandaloneTasksOnly() {
        queryTestUtils.buildTask().withId(TASK_ID_1).buildAndSave();

        queryTestUtils.buildProcessInstance().withTasks(queryTestUtils.buildTask()).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder().onlyStandalone();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnRootTasksOnly() {
        TaskEntity rootTask = queryTestUtils.buildTask().withId(TASK_ID_1).buildAndSave();
        queryTestUtils.buildTask().withParentTask(rootTask).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder().onlyRoot();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_returnTasksFilteredByNameContains() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withName("Darth Vader").buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withName("Frodo Baggins").buildAndSave();
        queryTestUtils.buildTask().withName("Duke Leto").buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder().withName("darth", "baggins");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByDescriptionContains() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withDescription("Darth Vader").buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withDescription("Frodo Baggins").buildAndSave();
        queryTestUtils.buildTask().withDescription("Duke Leto").buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withDescription("darth", "baggins");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByProcessDefinitionName() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withProcessDefinitionName("name1").buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withProcessDefinitionName("name2").buildAndSave();
        queryTestUtils.buildTask().withProcessDefinitionName("name3").buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessDefinitionName("name1", "name2");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByPriority() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withPriority(1).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withPriority(2).buildAndSave();
        queryTestUtils.buildTask().withPriority(3).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder().withPriority(1, 2);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByStatus() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withStatus(Task.TaskStatus.ASSIGNED).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withStatus(Task.TaskStatus.CANCELLED).buildAndSave();
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.COMPLETED).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withStatus(Task.TaskStatus.ASSIGNED, Task.TaskStatus.CANCELLED);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByCompletedBy() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withCompletedBy("Jimmy Page").buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withCompletedBy("Robert Plant").buildAndSave();
        queryTestUtils.buildTask().withCompletedBy("John Bonham").buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withCompletedBy("Jimmy Page", "Robert Plant");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByAssignee() {
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID_1)
            .withAssignee("Kimi Raikkonen")
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID_2)
            .withAssignee("Lewis Hamilton")
            .buildAndSave();
        queryTestUtils.buildTask().withOwner(CURRENT_USER).withAssignee("Sebastian Vettel").buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withAssignees("Kimi Raikkonen", "Lewis Hamilton");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByCreatedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withCreatedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withCreatedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCreatedDate(new Date(500)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withCreatedFrom(new Date(900));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByCreatedTo() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withCreatedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withCreatedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCreatedDate(new Date(3000)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withCreatedTo(new Date(2500));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByLastModifiedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withLastModifiedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withLastModifiedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withLastModifiedDate(new Date(500)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withLastModifiedFrom(new Date(900));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByLastModifiedTo() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withLastModifiedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withLastModifiedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withLastModifiedDate(new Date(3000)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withLastModifiedTo(new Date(2500));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByLastClaimedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withClaimedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withClaimedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withClaimedDate(new Date(500)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withLastClaimedFrom(new Date(900));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByLastClaimedTo() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withClaimedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withClaimedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withClaimedDate(new Date(3000)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withLastClaimedTo(new Date(2500));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByDueDateFrom() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withDueDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withDueDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withDueDate(new Date(500)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withDueDateFrom(new Date(900));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByDueDateTo() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withDueDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withDueDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withDueDate(new Date(3000)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withDueDateTo(new Date(2500));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByCompletedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withCompletedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withCompletedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCompletedDate(new Date(500)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withCompletedFrom(new Date(900));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByCompletedTo() {
        queryTestUtils.buildTask().withId(TASK_ID_1).withCompletedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(TASK_ID_2).withCompletedDate(new Date(2000)).buildAndSave();
        queryTestUtils.buildTask().withCompletedDate(new Date(3000)).buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withCompletedTo(new Date(2500));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByCandidateUserId() {
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID_1)
            .withTaskCandidateUsers("user1")
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID_2)
            .withTaskCandidateUsers("user2")
            .buildAndSave();
        queryTestUtils.buildTask().withOwner(CURRENT_USER).withTaskCandidateUsers("user3").buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withCandidateUserId("user1", "user2");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasksFilteredByCandidateGroupId() {
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID_1)
            .withTaskCandidateGroups("group1")
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID_2)
            .withTaskCandidateGroups("group2")
            .buildAndSave();
        queryTestUtils.buildTask().withOwner(CURRENT_USER).withTaskCandidateGroups("group3").buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withCandidateGroupId("group1", "group2");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnBadRequest_whenFilterIsIllegal() {
        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withProcessVariableFilters(
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.BOOLEAN,
                    String.valueOf(true),
                    FilterOperator.LIKE
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(400);
    }

    @Test
    void should_returnTasks_sortedBy_RootFields() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withName("task1")
            .withPriority(3)
            .withStatus(Task.TaskStatus.ASSIGNED)
            .withLastModifiedDate(new Date(1000))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withName("task2")
            .withPriority(1)
            .withStatus(Task.TaskStatus.CANCELLED)
            .withLastModifiedDate(new Date(3000))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(TASK_ID_3)
            .withName("task3")
            .withPriority(2)
            .withStatus(Task.TaskStatus.COMPLETED)
            .withLastModifiedDate(new Date(2000))
            .buildAndSave();

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withSort(new CloudRuntimeEntitySort("name", Sort.Direction.ASC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_2, TASK_ID_3));

        taskSearchRequestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(new CloudRuntimeEntitySort("priority", Sort.Direction.DESC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_3, TASK_ID_2));

        taskSearchRequestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(new CloudRuntimeEntitySort("status", Sort.Direction.ASC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_2, TASK_ID_3));

        taskSearchRequestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(new CloudRuntimeEntitySort("lastModified", Sort.Direction.DESC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2, TASK_ID_3, TASK_ID_1));
    }

    @Test
    void should_returnTasks_sortedBy_StringProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "cool"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "amazing"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "beautiful"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_3))
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(
                new CloudRuntimeEntitySort(
                    VAR_NAME,
                    Sort.Direction.ASC,
                    true,
                    List.of(PROCESS_DEFINITION_KEY),
                    VariableType.STRING
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2, TASK_ID_3, TASK_ID_1));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.STRING
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_3, TASK_ID_2));
    }

    @Test
    void should_returnTasks_sortedBy_IntegerProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 2))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 10))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 5))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_3))
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(
                new CloudRuntimeEntitySort(
                    VAR_NAME,
                    Sort.Direction.ASC,
                    true,
                    List.of(PROCESS_DEFINITION_KEY),
                    VariableType.INTEGER
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_3, TASK_ID_2));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.INTEGER
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2, TASK_ID_3, TASK_ID_1));
    }

    @Test
    void should_returnTasks_sortedBy_BigdecimalProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, 2.1))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, 10.2))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, 5.3))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_3))
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(
                new CloudRuntimeEntitySort(
                    VAR_NAME,
                    Sort.Direction.ASC,
                    true,
                    List.of(PROCESS_DEFINITION_KEY),
                    VariableType.BIGDECIMAL
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_3, TASK_ID_2));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.BIGDECIMAL
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2, TASK_ID_3, TASK_ID_1));
    }

    @Test
    void should_returnTasks_sortedBy_DateProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-03"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_3))
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(
                new CloudRuntimeEntitySort(
                    VAR_NAME,
                    Sort.Direction.ASC,
                    true,
                    List.of(PROCESS_DEFINITION_KEY),
                    VariableType.DATE
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_2, TASK_ID_3));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.DATE
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_3, TASK_ID_2, TASK_ID_1));
    }

    @Test
    void should_returnTasks_sortedBy_DatetimeProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:10:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_3))
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(
                new CloudRuntimeEntitySort(
                    VAR_NAME,
                    Sort.Direction.ASC,
                    true,
                    List.of(PROCESS_DEFINITION_KEY),
                    VariableType.DATETIME
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2, TASK_ID_1, TASK_ID_3));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.DATETIME
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_3, TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_sortedBy_BooleanProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, false))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(
                new CloudRuntimeEntitySort(
                    VAR_NAME,
                    Sort.Direction.ASC,
                    true,
                    List.of(PROCESS_DEFINITION_KEY),
                    VariableType.BOOLEAN
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_2, TASK_ID_1));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.BOOLEAN
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1, TASK_ID_2));
    }

    @Test
    void should_returnTasks_withSortedElementsFirst() {
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "cool"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1).withStatus(Task.TaskStatus.ASSIGNED))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_3).withStatus(Task.TaskStatus.CREATED))
            .buildAndSave();

        String taskId4 = "taskId4";
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "amazing"))
            .withTasks(queryTestUtils.buildTask().withId(taskId4))
            .buildAndSave();

        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(new CloudRuntimeEntitySort("status", Sort.Direction.ASC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(4))
            .body(TASK_IDS_JSON_PATH + "[0]", is(TASK_ID_1))
            .body(TASK_IDS_JSON_PATH + "[1]", is(TASK_ID_3));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(new CloudRuntimeEntitySort("status", Sort.Direction.DESC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(4))
            .body(TASK_IDS_JSON_PATH + "[0]", is(TASK_ID_3))
            .body(TASK_IDS_JSON_PATH + "[1]", is(TASK_ID_1));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.ASC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.STRING
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(4))
            .body(TASK_IDS_JSON_PATH + "[0]", is(taskId4))
            .body(TASK_IDS_JSON_PATH + "[1]", is(TASK_ID_1));

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        VariableType.STRING
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(4))
            .body(TASK_IDS_JSON_PATH + "[0]", is(TASK_ID_1))
            .body(TASK_IDS_JSON_PATH + "[1]", is(taskId4));
    }

    @Test
    void should_returnBadRequest_when_sortParameterIsInvalid() {
        TaskSearchRequestBuilder requestBuilder = new TaskSearchRequestBuilder()
            .withSort(new CloudRuntimeEntitySort(VAR_NAME, Sort.Direction.ASC, true, null, VariableType.STRING));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(400);

        requestBuilder =
            new TaskSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.ASC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY),
                        null
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(400);
    }

    @Test
    void should_returnFilteredPaginatedAndSortedTasks() {
        for (int i = 0; i < 10; i++) {
            queryTestUtils
                .buildTask()
                .withAssignee(CURRENT_USER)
                .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value"))
                .withId(String.valueOf(i))
                .buildAndSave();
            queryTestUtils.buildTask().withAssignee(CURRENT_USER).buildAndSave();
        }

        TaskSearchRequestBuilder taskSearchRequestBuilder = new TaskSearchRequestBuilder()
            .withTaskVariableFilters(
                new VariableFilter(null, VAR_NAME, VariableType.STRING, "value", FilterOperator.EQUALS)
            )
            .withSort(new CloudRuntimeEntitySort("id", Sort.Direction.ASC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .param("maxItems", 4)
            .param("skipCount", 0)
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(4))
            .body(TASK_IDS_JSON_PATH, contains("0", "1", "2", "3"))
            .body("page.totalElements", is(10))
            .body("page.totalPages", is(3))
            .body("page.size", is(4))
            .body("page.number", is(0));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .param("maxItems", 4)
            .param("skipCount", 4)
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(4))
            .body(TASK_IDS_JSON_PATH, contains("4", "5", "6", "7"))
            .body("page.totalElements", is(10))
            .body("page.totalPages", is(3))
            .body("page.size", is(4))
            .body("page.number", is(1));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .param("maxItems", 4)
            .param("skipCount", 8)
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, contains("8", "9"))
            .body("page.totalElements", is(10))
            .body("page.totalPages", is(3))
            .body("page.size", is(4))
            .body("page.number", is(2));
    }
}
