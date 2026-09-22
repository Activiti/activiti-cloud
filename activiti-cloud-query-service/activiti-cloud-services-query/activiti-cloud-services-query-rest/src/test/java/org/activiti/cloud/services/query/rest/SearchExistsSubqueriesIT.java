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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.postProcessors;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.activiti.cloud.services.query.rest.SearchExistsSubqueriesIT.CURRENT_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.app.filter.FilterOperator;
import org.activiti.cloud.services.query.app.filter.VariableFilter;
import org.activiti.cloud.services.query.app.filter.VariableType;
import org.activiti.cloud.services.query.app.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.activiti.cloud.services.query.util.TaskSearchRequestBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Covers the {@link QueryFeatureToggles#FEATURE_EXISTS_SUBQUERIES} variant of the variable filters,
 * for both {@link org.activiti.cloud.services.query.app.specification.TaskSpecification} and
 * {@link org.activiti.cloud.services.query.app.specification.ProcessInstanceSpecification}: the
 * process variable filters live in the shared
 * {@code SpecificationSupport#applyProcessVariableFilters}, so the two searches correlate on a
 * different association ({@code task_process_variable} join table vs. a plain foreign key) and are
 * worth asserting separately.
 *
 * <p>Two things are asserted: that the {@code EXISTS} form selects exactly the same tasks as the
 * legacy {@code group by} / {@code having max(case when ... end)} form, and that it actually removes
 * the aggregation from the generated SQL — which is the whole point of the change, since the
 * {@code group by} prevents the planner from using the {@code order by} index and forces the count
 * query onto {@code count(*) over()}.
 */
@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.activiti.cloud.services.query.rest.SearchExistsSubqueriesIT$CapturedStatements",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@WithMockUser(username = CURRENT_USER, roles = "ACTIVITI_USER")
class SearchExistsSubqueriesIT {

    static final String CURRENT_USER = "testuser";

    private static final String SEARCH_ENDPOINT = "/v1/tasks/search";
    private static final String COUNT_ENDPOINT = "/v1/tasks/count";
    private static final String PROCESS_INSTANCE_SEARCH_ENDPOINT = "/v1/process-instances/search";
    private static final String PROCESS_INSTANCE_COUNT_ENDPOINT = "/v1/process-instances/count";
    private static final String PROCESS_DEFINITION_KEY = "process-definition-key";
    private static final String PROCESS_DEFINITION_NAME = "Expense Approval";
    private static final String VAR_NAME = "var-name";
    private static final String TASK_ID_1 = "taskId1";
    private static final String TASK_ID_2 = "taskId2";
    private static final String TASKS_JSON_PATH = "_embedded.tasks";
    private static final String TASK_IDS_JSON_PATH = "_embedded.tasks.id";
    private static final String PROCESS_INSTANCES_JSON_PATH = "_embedded.processInstances";
    private static final String PROCESS_INSTANCE_IDS_JSON_PATH = "_embedded.processInstances.id";
    private static final String PROCESS_INSTANCE_ID_1 = "processInstanceId1";
    private static final String PROCESS_INSTANCE_ID_2 = "processInstanceId2";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @BeforeEach
    void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
        CapturedStatements.clear();
    }

    @AfterEach
    void cleanUp() {
        FeatureToggleHolder.reset();
        queryTestUtils.cleanUp();
    }

    private static void setExistsSubqueriesEnabled(boolean enabled) {
        if (enabled) {
            FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_EXISTS_SUBQUERIES::equals);
        } else {
            FeatureToggleHolder.reset();
        }
    }

    /**
     * Reproduces the shape of the slow search: a {@code like} on the process definition name, a
     * {@code like} on a process variable value and a sort on a task column.
     */
    private void givenTwoTasksOneMatchingTheProcessVariableFilter() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withProcessDefinitionName(PROCESS_DEFINITION_NAME)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "matching-value"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withProcessDefinitionName(PROCESS_DEFINITION_NAME)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();
    }

    private static TaskSearchRequestBuilder processVariableLikeRequest() {
        return new TaskSearchRequestBuilder()
            .withSort(new CloudRuntimeEntitySort("createdDate", Sort.Direction.DESC, false, null, null))
            .withProcessDefinitionName(PROCESS_DEFINITION_NAME.toLowerCase())
            .withProcessVariableFilters(
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.STRING,
                    "matching",
                    FilterOperator.LIKE
                )
            );
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void should_returnSameTasks_filteredByProcessVariable_regardlessOfTheToggle(boolean existsSubqueriesEnabled) {
        setExistsSubqueriesEnabled(existsSubqueriesEnabled);
        givenTwoTasksOneMatchingTheProcessVariableFilter();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processVariableLikeRequest().buildJson())
            .when()
            .post(SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processVariableLikeRequest().buildJson())
            .when()
            .post(COUNT_ENDPOINT)
            .then()
            .statusCode(200)
            .body(equalTo("1"));
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void should_returnSameTasks_filteredByTaskVariable_regardlessOfTheToggle(boolean existsSubqueriesEnabled) {
        setExistsSubqueriesEnabled(existsSubqueriesEnabled);
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_1)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "matching-value"))
            .buildAndSave();
        queryTestUtils
            .buildTask()
            .withId(TASK_ID_2)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .buildAndSave();

        TaskSearchRequestBuilder request = new TaskSearchRequestBuilder().withTaskVariableFilters(
            new VariableFilter(null, VAR_NAME, VariableType.STRING, "matching-value", FilterOperator.EQUALS)
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request.buildJson())
            .when()
            .post(SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    /**
     * A task whose process has no variable with the filtered name must be excluded by both forms:
     * {@code max(...)} is {@code null} there, which makes the comparison unknown, exactly like a
     * false {@code EXISTS}.
     */
    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void should_excludeTasksWithoutTheVariable_onNotEquals(boolean existsSubqueriesEnabled) {
        setExistsSubqueriesEnabled(existsSubqueriesEnabled);
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_1))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput("another-var", VariableType.STRING, "whatever"))
            .withTasks(queryTestUtils.buildTask().withId(TASK_ID_2))
            .buildAndSave();

        TaskSearchRequestBuilder request = new TaskSearchRequestBuilder().withProcessVariableFilters(
            new VariableFilter(
                PROCESS_DEFINITION_KEY,
                VAR_NAME,
                VariableType.STRING,
                "excluded-value",
                FilterOperator.NOT_EQUALS
            )
        );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request.buildJson())
            .when()
            .post(SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASK_IDS_JSON_PATH, contains(TASK_ID_1));
    }

    @Test
    void should_groupByAndAggregate_whenToggleIsOff() {
        setExistsSubqueriesEnabled(false);
        givenTwoTasksOneMatchingTheProcessVariableFilter();
        CapturedStatements.clear();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processVariableLikeRequest().buildJson())
            .when()
            .post(SEARCH_ENDPOINT)
            .then()
            .statusCode(200);

        String searchSql = CapturedStatements.findSelectFrom("from task ");
        assertThat(searchSql).contains("group by").contains("having").contains("max(");
    }

    @Test
    void should_useExistsWithoutAggregation_whenToggleIsOn() {
        setExistsSubqueriesEnabled(true);
        givenTwoTasksOneMatchingTheProcessVariableFilter();
        CapturedStatements.clear();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processVariableLikeRequest().buildJson())
            .when()
            .post(SEARCH_ENDPOINT)
            .then()
            .statusCode(200);

        String searchSql = CapturedStatements.findSelectFrom("from task ");
        assertThat(searchSql)
            .containsPattern("exists\\s*\\(")
            .doesNotContain("group by")
            .doesNotContain("having")
            .doesNotContain("max(");
    }

    @Test
    void should_usePlainCount_whenToggleIsOn() {
        setExistsSubqueriesEnabled(true);
        givenTwoTasksOneMatchingTheProcessVariableFilter();
        CapturedStatements.clear();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processVariableLikeRequest().buildJson())
            .when()
            .post(COUNT_ENDPOINT)
            .then()
            .statusCode(200)
            .body(equalTo("1"));

        String countSql = CapturedStatements.findSelectFrom("from task ");
        assertThat(countSql).contains("count(").doesNotContain("over()").doesNotContain("group by");
    }

    private void givenTwoProcessInstancesOneMatchingTheProcessVariableFilter() {
        queryTestUtils
            .buildProcessInstance()
            .withId(PROCESS_INSTANCE_ID_1)
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withProcessDefinitionName(PROCESS_DEFINITION_NAME)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "matching-value"))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withId(PROCESS_INSTANCE_ID_2)
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withProcessDefinitionName(PROCESS_DEFINITION_NAME)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .buildAndSave();
    }

    private static ProcessInstanceSearchRequestBuilder processInstanceVariableLikeRequest() {
        return new ProcessInstanceSearchRequestBuilder().withProcessVariableFilters(
            new VariableFilter(PROCESS_DEFINITION_KEY, VAR_NAME, VariableType.STRING, "matching", FilterOperator.LIKE)
        );
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void should_returnSameProcessInstances_filteredByProcessVariable_regardlessOfTheToggle(
        boolean existsSubqueriesEnabled
    ) {
        setExistsSubqueriesEnabled(existsSubqueriesEnabled);
        givenTwoProcessInstancesOneMatchingTheProcessVariableFilter();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processInstanceVariableLikeRequest().buildJson())
            .when()
            .post(PROCESS_INSTANCE_SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, contains(PROCESS_INSTANCE_ID_1));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processInstanceVariableLikeRequest().buildJson())
            .when()
            .post(PROCESS_INSTANCE_COUNT_ENDPOINT)
            .then()
            .statusCode(200)
            .body(equalTo("1"));
    }

    /**
     * A process instance without any variable of that name must be excluded by both forms, the same
     * way it is for tasks.
     */
    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void should_excludeProcessInstancesWithoutTheVariable_onNotEquals(boolean existsSubqueriesEnabled) {
        setExistsSubqueriesEnabled(existsSubqueriesEnabled);
        queryTestUtils
            .buildProcessInstance()
            .withId(PROCESS_INSTANCE_ID_1)
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withId(PROCESS_INSTANCE_ID_2)
            .withInitiator(CURRENT_USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput("another-var", VariableType.STRING, "whatever"))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder request =
            new ProcessInstanceSearchRequestBuilder().withProcessVariableFilters(
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.STRING,
                    "excluded-value",
                    FilterOperator.NOT_EQUALS
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request.buildJson())
            .when()
            .post(PROCESS_INSTANCE_SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, contains(PROCESS_INSTANCE_ID_1));
    }

    @Test
    void should_groupByAndAggregateProcessInstances_whenToggleIsOff() {
        setExistsSubqueriesEnabled(false);
        givenTwoProcessInstancesOneMatchingTheProcessVariableFilter();
        CapturedStatements.clear();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processInstanceVariableLikeRequest().buildJson())
            .when()
            .post(PROCESS_INSTANCE_SEARCH_ENDPOINT)
            .then()
            .statusCode(200);

        String searchSql = CapturedStatements.findSelectFrom("from process_instance ");
        assertThat(searchSql).contains("group by").contains("having").contains("max(");
    }

    @Test
    void should_useExistsWithoutAggregationForProcessInstances_whenToggleIsOn() {
        setExistsSubqueriesEnabled(true);
        givenTwoProcessInstancesOneMatchingTheProcessVariableFilter();
        CapturedStatements.clear();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processInstanceVariableLikeRequest().buildJson())
            .when()
            .post(PROCESS_INSTANCE_SEARCH_ENDPOINT)
            .then()
            .statusCode(200);

        String searchSql = CapturedStatements.findSelectFrom("from process_instance ");
        assertThat(searchSql)
            .containsPattern("exists\\s*\\(")
            .doesNotContain("group by")
            .doesNotContain("having")
            .doesNotContain("max(");
    }

    @Test
    void should_usePlainCountForProcessInstances_whenToggleIsOn() {
        setExistsSubqueriesEnabled(true);
        givenTwoProcessInstancesOneMatchingTheProcessVariableFilter();
        CapturedStatements.clear();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(processInstanceVariableLikeRequest().buildJson())
            .when()
            .post(PROCESS_INSTANCE_COUNT_ENDPOINT)
            .then()
            .statusCode(200)
            .body(equalTo("1"));

        String countSql = CapturedStatements.findSelectFrom("from process_instance ");
        assertThat(countSql).contains("count(").doesNotContain("over()").doesNotContain("group by");
    }

    /**
     * Hibernate {@link StatementInspector} collecting the SQL issued by the current test. Registered
     * through {@code hibernate.session_factory.statement_inspector}, hence the public no-arg
     * constructor and the static state.
     */
    public static class CapturedStatements implements StatementInspector {

        private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

        @Override
        public String inspect(String sql) {
            STATEMENTS.add(sql);
            return sql;
        }

        static void clear() {
            STATEMENTS.clear();
        }

        /**
         * @return the last captured {@code select} containing the given fragment, lower-cased
         */
        static String findSelectFrom(String fragment) {
            String found = STATEMENTS.stream()
                .map(String::toLowerCase)
                .filter(sql -> sql.startsWith("select") && sql.contains(fragment))
                .reduce((_, second) -> second)
                .orElse(null);
            assertThat(found).as("no select statement containing '%s' was captured", fragment).isNotNull();
            return found;
        }
    }
}
