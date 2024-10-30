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
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.FilterOperator;
import org.activiti.cloud.services.query.rest.filter.VariableFilter;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContextConfiguration(classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class })
abstract class AbstractProcessInstanceEntitySearchControllerIT {

    private static final String PROCESS_DEFINITION_KEY = "process-def-key";
    private static final String VAR_NAME = "var-name";
    public static final String USER = "testuser";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected QueryTestUtils queryTestUtils;

    protected static final String PROCESS_INSTANCES_JSON_PATH = "_embedded.processInstances";
    protected static final String PROCESS_INSTANCE_IDS_JSON_PATH = "_embedded.processInstances.id";

    @BeforeEach
    void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
    }

    @AfterEach
    void cleanUp() {
        queryTestUtils.cleanUp();
    }

    protected abstract String getSearchEndpoint();

    @Test
    void should_returnProcessInstances_filteredByNameLike() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withName("Beautiful process instance name")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withName("Amazing process instance name")
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withName("Ugly process instance name").buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withNames("amazing", "beautiful");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByAppVersion() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withAppVersion("1.0.0")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withAppVersion("2.0.0")
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withAppVersion("3.0.0").buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withAppVersions("1.0.0", "2.0.0");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withLastModified(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withLastModified(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withLastModifiedFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withLastModified(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withLastModified(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withLastModifiedTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByStartFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withStartDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withStartDate(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withStartFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByStartTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withStartDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withStartDate(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withStartTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withCompletedDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withCompletedDate(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withCompletedFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withCompletedDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withCompletedDate(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withCompletedTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withSuspendedDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withSuspendedDate(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withSuspendedFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withSuspendedDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator(USER).withSuspendedDate(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withSuspendedTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstance_withoutProcessVariables() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance.getId()))
            .body(PROCESS_INSTANCES_JSON_PATH + "[0].variables", empty());
    }

    @Test
    void should_returnProcessInstances_withJustRequestedProcessVariables() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.INTEGER, 1),
                new QueryTestUtils.VariableInput("var3", VariableType.STRING, "value3"),
                new QueryTestUtils.VariableInput("var4", VariableType.BOOLEAN, true)
            )
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableKeys(
                new ProcessVariableKey(PROCESS_DEFINITION_KEY, VAR_NAME),
                new ProcessVariableKey(PROCESS_DEFINITION_KEY, "var3")
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance.getId()))
            .body(PROCESS_INSTANCES_JSON_PATH + "[0].variables", hasSize(2))
            .body(PROCESS_INSTANCES_JSON_PATH + "[0].variables.name", hasItem(VAR_NAME))
            .body(PROCESS_INSTANCES_JSON_PATH + "[0].variables.name", hasItem("var3"));
    }

    @Test
    void should_returnProcessInstance_filteredByVariable_whenAllFiltersMatch() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .buildAndSave();

        VariableFilter matchingFilter1 = new VariableFilter(
            processInstance.getProcessDefinitionKey(),
            VAR_NAME,
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

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(matchingFilter1, matchingFilter2);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance.getId()));
    }

    @Test
    void should_not_returnProcessInstance_filteredByVariable_whenOneFilterDoesNotMatch() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.STRING, "value2")
            )
            .buildAndSave();

        VariableFilter matchingFilter = new VariableFilter(
            processInstance.getProcessDefinitionKey(),
            VAR_NAME,
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        VariableFilter nonMatchingFilter = new VariableFilter(
            processInstance.getProcessDefinitionKey(),
            "var2",
            VariableType.STRING,
            "value3",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(matchingFilter, nonMatchingFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body("page.totalElements", equalTo(0));
    }

    @Test
    void should_returnProcessInstances_filteredByStringVariable_exactMatch() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.STRING,
            "value1",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByStringVariable_Contains() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "abcdefg"))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "other-value"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.STRING,
            "bcde",
            FilterOperator.LIKE
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByIntegerVariable_equals() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 1))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 2))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(1),
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByIntegerVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 10))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 2))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(2),
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByIntegerVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 10))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 2))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(2),
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByIntegerVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 2))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 10))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(10),
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByIntegerVariable_lessThanEqual() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 2))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 10))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            String.valueOf(10),
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByIntegerVariable_range() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 4))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 8))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 15))
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            "4",
            FilterOperator.GREATER_THAN
        );

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            "15",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(filterGt, filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));

        VariableFilter filterGtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            "4",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        VariableFilter filterLtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.INTEGER,
            "15",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        requestBuilder = new ProcessInstanceSearchRequestBuilder().withProcessVariableFilters(filterGtEq, filterLtEq);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance3.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByBigDecimalVariable_equals() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("1.1")))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("1.2")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "1.1",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByBigDecimalVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("10.1")))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("2.1")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "2.1",
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByBigDecimalVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("10.1")))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("2.1")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "2.1",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByBigDecimalVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("2.1")))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("10.1")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "10.1",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByBigDecimalVariable_lessThanEqual() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("2.1")))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("10.1")))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "10.1",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredBigdecimalVariable_range() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("4.8")))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("15.16")))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("23.42")))
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "4.8",
            FilterOperator.GREATER_THAN
        );

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "23.42",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(filterGt, filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));

        VariableFilter filterGtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "4.8",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        VariableFilter filterLtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BIGDECIMAL,
            "23.42",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        requestBuilder = new ProcessInstanceSearchRequestBuilder().withProcessVariableFilters(filterGtEq, filterLtEq);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance3.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDateVariable_equals() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDateVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDateVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDateVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-02",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDateVariable_lessThanEquals() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-02",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDateVariable_range() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-03"))
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.GREATER_THAN
        );

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-03",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(filterGt, filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));

        VariableFilter filterGtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-01",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        VariableFilter filterLtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATE,
            "2024-09-03",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        requestBuilder = new ProcessInstanceSearchRequestBuilder().withProcessVariableFilters(filterGtEq, filterLtEq);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance3.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDatetimeVariable_equals() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDatetimeVariable_greaterThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDatetimeVariable_greaterThanEqual() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDatetimeVariable_lessThan() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:12:00.000+00:00",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDatetimeVariable_lessThanEqual() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:12:00.000+00:00",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByDatetimeVariable_range() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:13:00.000+00:00")
            )
            .buildAndSave();

        VariableFilter filterGt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN
        );

        VariableFilter filterLt = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:13:00.000+00:00",
            FilterOperator.LESS_THAN
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(filterGt, filterLt);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));

        VariableFilter filterGtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:11:00.000+00:00",
            FilterOperator.GREATER_THAN_OR_EQUAL
        );

        VariableFilter filterLtEq = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.DATETIME,
            "2024-09-01T00:13:00.000+00:00",
            FilterOperator.LESS_THAN_OR_EQUAL
        );

        requestBuilder = new ProcessInstanceSearchRequestBuilder().withProcessVariableFilters(filterGtEq, filterLtEq);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance3.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByBooleanVariable() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, false))
            .buildAndSave();

        VariableFilter variableFilter = new VariableFilter(
            PROCESS_DEFINITION_KEY,
            VAR_NAME,
            VariableType.BOOLEAN,
            "true",
            FilterOperator.EQUALS
        );

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(variableFilter);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_sortedBy_RootFields() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withName("Nice name")
            .withInitiator(USER)
            .withStartDate(new Date(3000))
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withName("Good name")
            .withInitiator(USER)
            .withStartDate(new Date(2000))
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withName("Amazing name")
            .withInitiator(USER)
            .withStartDate(new Date(4000))
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withSort(new CloudRuntimeEntitySort("name", Sort.Direction.ASC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance3.getId(), processInstance2.getId(), processInstance1.getId())
            );

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
                .withSort(new CloudRuntimeEntitySort("startDate", Sort.Direction.DESC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance3.getId(), processInstance1.getId(), processInstance2.getId())
            );
    }

    @Test
    void should_returnProcessInstances_sortedBy_StringProcessVariable() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "cool"))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "amazing"))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "beautiful"))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance2.getId(), processInstance3.getId(), processInstance1.getId())
            );

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance1.getId(), processInstance3.getId(), processInstance2.getId())
            );
    }

    @Test
    void should_returnProcessInstances_sortedBy_IntegerProcessVariable() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 2))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 3))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.INTEGER, 1))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance3.getId(), processInstance1.getId(), processInstance2.getId())
            );

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance2.getId(), processInstance1.getId(), processInstance3.getId())
            );
    }

    @Test
    void should_returnProcessInstances_sortedBy_BigdecimalProcessVariable() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("2.1")))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("10.1")))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BIGDECIMAL, new BigDecimal("5.1")))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance1.getId(), processInstance3.getId(), processInstance2.getId())
            );

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance2.getId(), processInstance3.getId(), processInstance1.getId())
            );
    }

    @Test
    void should_returnProcessInstances_sortedBy_DateProcessVariable() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-03"))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-01"))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATE, "2024-09-02"))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance2.getId(), processInstance3.getId(), processInstance1.getId())
            );

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance1.getId(), processInstance3.getId(), processInstance2.getId())
            );
    }

    @Test
    void should_returnProcessInstances_sortedBy_DatetimeProcessVariable() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:11:00.000+00:00")
            )
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:10:00.000+00:00")
            )
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput(VAR_NAME, VariableType.DATETIME, "2024-09-01T00:12:00.000+00:00")
            )
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance2.getId(), processInstance1.getId(), processInstance3.getId())
            );

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                contains(processInstance3.getId(), processInstance1.getId(), processInstance2.getId())
            );
    }

    @Test
    void should_returnProcessInstances_sortedBy_BooleanProcessVariable() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, false))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.BOOLEAN, true))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                containsInRelativeOrder(processInstance2.getId(), processInstance1.getId())
            )
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                containsInRelativeOrder(processInstance2.getId(), processInstance3.getId())
            );

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                containsInRelativeOrder(processInstance1.getId(), processInstance2.getId())
            )
            .body(
                PROCESS_INSTANCE_IDS_JSON_PATH,
                containsInRelativeOrder(processInstance3.getId(), processInstance2.getId())
            );
    }

    @Test
    void should_returnProcessInstances_withSortedElementsFirst() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withAppName("Nice app")
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "cool"))
            .buildAndSave();

        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "amazing"))
            .buildAndSave();

        ProcessInstanceEntity processInstance3 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withAppName("Best app ever")
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput("var2", VariableType.INTEGER, 4))
            .buildAndSave();

        ProcessInstanceEntity processInstance4 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput("var2", VariableType.INTEGER, 3))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(4))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH + "[0]", is(processInstance2.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH + "[1]", is(processInstance1.getId()));

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        "var2",
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(4))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH + "[0]", is(processInstance3.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH + "[1]", is(processInstance4.getId()));

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
                .withSort(new CloudRuntimeEntitySort("appName", Sort.Direction.DESC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(4))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH + "[0]", is(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH + "[1]", is(processInstance3.getId()));
    }

    @Test
    void should_returnBadRequest_when_sortParameterIsInvalid() {
        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withSort(new CloudRuntimeEntitySort(VAR_NAME, Sort.Direction.ASC, true, null, VariableType.STRING));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(400);

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(400);
    }

    @Test
    void should_returnProcessInstances_sortedByProcessVariable_withDifferentProcessDefinitionKeys() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        queryTestUtils
            .buildProcessInstance()
            .withId(id1)
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "cool"))
            .buildAndSave();

        String otherProcDefKey = "other-process-definition-key";
        queryTestUtils
            .buildProcessInstance()
            .withId(id2)
            .withInitiator(USER)
            .withProcessDefinitionKey(otherProcDefKey)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "amazing"))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withSort(
                new CloudRuntimeEntitySort(
                    VAR_NAME,
                    Sort.Direction.ASC,
                    true,
                    List.of(PROCESS_DEFINITION_KEY, otherProcDefKey),
                    VariableType.STRING
                )
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, contains(id2, id1));

        requestBuilder =
            new ProcessInstanceSearchRequestBuilder()
                .withSort(
                    new CloudRuntimeEntitySort(
                        VAR_NAME,
                        Sort.Direction.DESC,
                        true,
                        List.of(PROCESS_DEFINITION_KEY, otherProcDefKey),
                        VariableType.STRING
                    )
                );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, contains(id1, id2));
    }

    @Test
    void should_returnFilteredPaginatedAndSortedProcessInstances() {
        for (int i = 0; i < 10; i++) {
            queryTestUtils
                .buildProcessInstance()
                .withId(String.valueOf(i))
                .withInitiator(USER)
                .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
                .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value"))
                .buildAndSave();
        }

        ProcessInstanceSearchRequestBuilder taskSearchRequestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableFilters(
                new VariableFilter(
                    PROCESS_DEFINITION_KEY,
                    VAR_NAME,
                    VariableType.STRING,
                    "value",
                    FilterOperator.EQUALS
                )
            )
            .withSort(new CloudRuntimeEntitySort("id", Sort.Direction.ASC, false, null, null));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequestBuilder.buildJson())
            .param("maxItems", 4)
            .param("skipCount", 0)
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(4))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, contains("0", "1", "2", "3"))
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(4))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, contains("4", "5", "6", "7"))
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
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, contains("8", "9"))
            .body("page.totalElements", is(10))
            .body("page.totalPages", is(3))
            .body("page.size", is(4))
            .body("page.number", is(2));
    }
}
