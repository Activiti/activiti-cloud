package org.activiti.cloud.services.query.rest;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.postProcessors;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

public abstract class AbstractTaskControllerIT {

    protected static final String CURRENT_USER = "testuser";
    protected static final String VAR_NAME = "var-name";
    protected static final String PROCESS_DEFINITION_KEY = "process-definition-key";
    protected static final String TASK_ID = "taskId";
    protected static final String OTHER_TASK_ID = "otherTaskId";
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
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
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
            .param("variableKeys", processInstance.getProcessDefinitionKey() + "/var1")
            .param("variables.name", "taskVar1")
            .param("variables.value", "taskValue1")
            .when()
            .get(getSearchEndpointHttpGet())
            .then()
            .statusCode(200)
            .body("_embedded.tasks", hasSize(1));
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
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
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
            .withId(TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
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
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByStringTaskVariable_exactMatch() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByStringProcessVariable_containsInAnyOrder() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "Eren Jaeger"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByTaskProcessVariable_containsInAnyOrder() {
        queryTestUtils
            .buildProcessInstance()
            .withTasks(
                queryTestUtils
                    .buildTask()
                    .withId(TASK_ID)
                    .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "Gray Fox")),
                queryTestUtils
                    .buildTask()
                    .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 43))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 41))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 41))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 84))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByIntegerTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 42))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.42")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.2")))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("14.3")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.1")))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("84.2")))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByBigDecimalTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("42.1")))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-03"))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-04"))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-08-02"))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTimeProcessVariable_equals() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_equals() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_gt_gte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_gt_gte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByDateTimeProcessVariable_lt_lte() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:12:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByDateTimeTaskVariable_lt_lte() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTimeProcessVariable_range() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:14:00.000+00:00")
            )
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasks_filteredByDateTimeTaskVariable_range() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-08-02T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByBooleanProcessVariable() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, false))
            .withTasks(queryTestUtils.buildTask().withId(OTHER_TASK_ID))
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByBooleanTaskVariable() {
        queryTestUtils
            .buildTask()
            .withId(TASK_ID)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .buildAndSave();

        queryTestUtils
            .buildTask()
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));

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
            .body(TASK_IDS_JSON_PATH, contains(OTHER_TASK_ID));
    }

    @Test
    void should_returnStandaloneTasksOnly() {
        queryTestUtils.buildTask().withId(TASK_ID).buildAndSave();

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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnRootTasksOnly() {
        TaskEntity rootTask = queryTestUtils.buildTask().withId(TASK_ID).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByNameContains() {
        queryTestUtils.buildTask().withId(TASK_ID).withName("Darth Vader").buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withName("Frodo Baggins").buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByDescriptionContains() {
        queryTestUtils.buildTask().withId(TASK_ID).withDescription("Darth Vader").buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withDescription("Frodo Baggins").buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByProcessDefinitionName() {
        queryTestUtils.buildTask().withId(TASK_ID).withProcessDefinitionName("name1").buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withProcessDefinitionName("name2").buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByPriority() {
        queryTestUtils.buildTask().withId(TASK_ID).withPriority(1).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withPriority(2).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByStatus() {
        queryTestUtils.buildTask().withId(TASK_ID).withStatus(Task.TaskStatus.ASSIGNED).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withStatus(Task.TaskStatus.CANCELLED).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByCompletedBy() {
        queryTestUtils.buildTask().withId(TASK_ID).withCompletedBy("Jimmy Page").buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withCompletedBy("Robert Plant").buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByAssignee() {
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID)
            .withAssignee("Kimi Raikkonen")
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByCreatedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID).withCreatedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withCreatedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByCreatedTo() {
        queryTestUtils.buildTask().withId(TASK_ID).withCreatedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withCreatedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByLastModifiedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID).withLastModifiedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withLastModifiedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByLastModifiedTo() {
        queryTestUtils.buildTask().withId(TASK_ID).withLastModifiedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withLastModifiedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByLastClaimedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID).withClaimedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withClaimedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByLastClaimedTo() {
        queryTestUtils.buildTask().withId(TASK_ID).withClaimedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withClaimedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByDueDateFrom() {
        queryTestUtils.buildTask().withId(TASK_ID).withDueDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withDueDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByDueDateTo() {
        queryTestUtils.buildTask().withId(TASK_ID).withDueDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withDueDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByCompletedFrom() {
        queryTestUtils.buildTask().withId(TASK_ID).withCompletedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withCompletedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByCompletedTo() {
        queryTestUtils.buildTask().withId(TASK_ID).withCompletedDate(new Date(1000)).buildAndSave();
        queryTestUtils.buildTask().withId(OTHER_TASK_ID).withCompletedDate(new Date(2000)).buildAndSave();
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByCandidateUserId() {
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID)
            .withTaskCandidateUsers("user1")
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
    }

    @Test
    void should_returnTasksFilteredByCandidateGroupId() {
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(TASK_ID)
            .withTaskCandidateGroups("group1")
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withOwner(CURRENT_USER)
            .withId(OTHER_TASK_ID)
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
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(TASK_ID, OTHER_TASK_ID));
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
}
