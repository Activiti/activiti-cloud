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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Date;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableKey;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ContextConfiguration(classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class })
abstract class AbstractProcessInstanceEntitySearchControllerIT {

    private static final String PROCESS_DEFINITION_KEY = "process-def-key";

    public static final String USER = "testuser";

    @Autowired
    private WebApplicationContext context;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    protected QueryTestUtils queryTestUtils;

    protected final String processInstanceJsonPath = "_embedded.processInstances";
    protected final String processInstanceIdJsonPath = "_embedded.processInstances.id";

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
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(2))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()))
            .body(processInstanceIdJsonPath, hasItem(processInstance2.getId()));
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
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(2))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()))
            .body(processInstanceIdJsonPath, hasItem(processInstance2.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withLastModified(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withLastModified(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withLastModifiedFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByLastModifiedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withLastModified(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withLastModified(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withLastModifiedTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByStartFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withStartDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withStartDate(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withStartFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByStartTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withStartDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withStartDate(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withStartTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withCompletedDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withCompletedDate(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withCompletedFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredByCompletedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withCompletedDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withCompletedDate(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withCompletedTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedFrom() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withSuspendedDate(new Date(2000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withSuspendedDate(new Date(1000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withSuspendedFrom(new Date(1000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstances_filteredBySuspendedTo() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withSuspendedDate(new Date(1000))
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withSuspendedDate(new Date(2000)).buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withSuspendedTo(new Date(2000));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance1.getId()));
    }

    @Test
    void should_returnProcessInstance_withoutProcessVariables() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
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
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance.getId()))
            .body(processInstanceJsonPath + "[0].variables", empty());
    }

    @Test
    void should_returnProcessInstances_withJustRequestedProcessVariables() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(
                new QueryTestUtils.VariableInput("var1", VariableType.STRING, "value1"),
                new QueryTestUtils.VariableInput("var2", VariableType.INTEGER, 1),
                new QueryTestUtils.VariableInput("var3", VariableType.STRING, "value3"),
                new QueryTestUtils.VariableInput("var4", VariableType.BOOLEAN, true)
            )
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessVariableKeys(
                new ProcessVariableKey(PROCESS_DEFINITION_KEY, "var1"),
                new ProcessVariableKey(PROCESS_DEFINITION_KEY, "var3")
            );

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.build())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(processInstanceJsonPath, hasSize(1))
            .body(processInstanceIdJsonPath, hasItem(processInstance.getId()))
            .body(processInstanceJsonPath + "[0].variables", hasSize(2))
            .body(processInstanceJsonPath + "[0].variables.name", hasItem("var1"))
            .body(processInstanceJsonPath + "[0].variables.name", hasItem("var3"));
    }
}
