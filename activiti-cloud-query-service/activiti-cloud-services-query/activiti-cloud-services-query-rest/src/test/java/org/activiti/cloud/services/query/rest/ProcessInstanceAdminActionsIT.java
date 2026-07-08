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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@WithMockUser(username = "testuser", roles = "ACTIVITI_ADMIN")
class ProcessInstanceAdminActionsIT {

    private static final String PROCESS_DEFINITION_KEY = "process-def-key";
    private static final String VAR_NAME = "var-name";
    private static final String PROCESS_INSTANCES_ENDPOINT = "/admin/v1/process-instances";
    private static final String PROCESS_INSTANCES_JSON_PATH = "_embedded.processInstances";
    private static final String PROCESS_INSTANCE_IDS_JSON_PATH = "_embedded.processInstances.id";

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
    }

    @AfterEach
    void cleanUp() {
        queryTestUtils.cleanUp();
    }

    @Test
    void should_returnProcessInstances_whenPostingToAdminEndpointWithoutVariableKeys() {
        ProcessInstanceEntity process1 = queryTestUtils.buildProcessInstance().buildAndSave();
        ProcessInstanceEntity process2 = queryTestUtils.buildProcessInstance().buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(PROCESS_INSTANCES_ENDPOINT)
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, containsInAnyOrder(process1.getId(), process2.getId()));
    }

    @Test
    void should_returnProcessInstancesWithVariables_whenPostingToAdminEndpointWithVariableKeys() {
        ProcessInstanceEntity process = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"))
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"variableKeys\": [\"" + PROCESS_DEFINITION_KEY + "/" + VAR_NAME + "\"]}")
            .when()
            .post(PROCESS_INSTANCES_ENDPOINT)
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(process.getId()))
            .body(PROCESS_INSTANCES_JSON_PATH + "[0].variables", hasSize(1))
            .body(PROCESS_INSTANCES_JSON_PATH + "[0].variables.name", hasItem(VAR_NAME));
    }

    @Test
    void should_deleteAllProcessInstances_whenDeletingAdminEndpoint() {
        ProcessInstanceEntity processInstance = queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"))
            .withTasks(
                queryTestUtils
                    .buildTask()
                    .withTaskCandidateUsers("user1")
                    .withTaskCandidateGroups("group1")
                    .withVariables(new QueryTestUtils.VariableInput("taskVar", VariableType.STRING, "taskValue"))
            )
            .buildAndSave();
        queryTestUtils.buildProcessInstance().subprocessOf(processInstance).buildAndSave();
        queryTestUtils.buildProcessInstance().buildAndSave();

        given().when().delete(PROCESS_INSTANCES_ENDPOINT).then().statusCode(200);
    }
}
