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
package org.activiti.cloud.services.query.rest.count;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.postProcessors;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.stream.Stream;
import org.activiti.QueryRestTestApplication;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
@WithMockUser(username = CountControllerIT.CURRENT_USER, roles = "ACTIVITI_USER")
class CountControllerIT {

    static final String CURRENT_USER = "testuser";
    private static final String OTHER_USER = "other-user";

    private static final String COUNT_ENDPOINT = "/v1/count";
    private static final String ADMIN_COUNT_ENDPOINT = "/admin/v1/count";

    private static final String COUNT_REQUEST_BODY = """
        {
          "TASK": [ { "status": ["ASSIGNED"] } ],
          "PROCESS_INSTANCE": [ { "status": ["RUNNING"] } ]
        }""";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @BeforeEach
    void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
        seedData();
    }

    @AfterEach
    void cleanUp() {
        queryTestUtils.cleanUp();
    }

    private void seedData() {
        //Visible to CURRENT_USER (assignee)
        queryTestUtils.buildTask().withAssignee(CURRENT_USER).withStatus(Task.TaskStatus.ASSIGNED).buildAndSave();
        queryTestUtils.buildTask().withAssignee(CURRENT_USER).withStatus(Task.TaskStatus.ASSIGNED).buildAndSave();
        //NOT visible to CURRENT_USER (assigned to someone else)
        queryTestUtils.buildTask().withAssignee(OTHER_USER).withStatus(Task.TaskStatus.ASSIGNED).buildAndSave();

        //CREATED, unassigned, no candidates -> visible to any user
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave();
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave();
        queryTestUtils.buildTask().withStatus(Task.TaskStatus.CREATED).buildAndSave();

        //Visible to CURRENT_USER (initiator)
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(CURRENT_USER)
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .buildAndSave();
        //NOT visible to CURRENT_USER (initiated by someone else, no tasks)
        queryTestUtils
            .buildProcessInstance()
            .withInitiator(OTHER_USER)
            .withStatus(ProcessInstance.ProcessInstanceStatus.RUNNING)
            .buildAndSave();
    }

    @Test
    void should_returnCountsKeyedByStatus_restrictedToCurrentUser() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(COUNT_REQUEST_BODY)
            .when()
            .post(COUNT_ENDPOINT)
            .then()
            .statusCode(200)
            .body("TASK.ASSIGNED", equalTo(2))
            .body("PROCESS_INSTANCE.RUNNING", equalTo(1));
    }

    @Test
    @WithMockUser(username = CURRENT_USER, roles = "ACTIVITI_ADMIN")
    void should_returnUnrestrictedCounts_onAdminEndpoint() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(COUNT_REQUEST_BODY)
            .when()
            .post(ADMIN_COUNT_ENDPOINT)
            .then()
            .statusCode(200)
            .body("TASK.ASSIGNED", equalTo(3))
            .body("PROCESS_INSTANCE.RUNNING", equalTo(2));
    }

    @Test
    void should_matchLegacyCountEndpoints() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{ \"status\": [\"ASSIGNED\"] }")
            .when()
            .post("/v1/tasks/count")
            .then()
            .statusCode(200)
            .body(equalTo("2"));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{ \"status\": [\"RUNNING\"] }")
            .when()
            .post("/v1/process-instances/count")
            .then()
            .statusCode(200)
            .body(equalTo("1"));
    }

    @Test
    void should_returnCountsForMultipleFiltersPerResourceType_withExtraCriteriaAndSort() {
        String body = """
            {
              "TASK": [
                { "status": ["ASSIGNED"], "assignee": ["%s"] },
                { "status": ["CREATED"], "sort": { "field": "createdDate", "direction": "desc", "isProcessVariable": false } }
              ]
            }""".formatted(CURRENT_USER);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .when()
            .post(COUNT_ENDPOINT)
            .then()
            .statusCode(200)
            .body("TASK.ASSIGNED", equalTo(2))
            .body("TASK.CREATED", equalTo(3));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCountRequests")
    void should_return400_whenRequestIsInvalid(String reason, String body) {
        given().contentType(MediaType.APPLICATION_JSON).body(body).when().post(COUNT_ENDPOINT).then().statusCode(400);
    }

    private static Stream<Arguments> invalidCountRequests() {
        return Stream.of(
            Arguments.of("resource type unknown", "{ \"UNKNOWN\": [ { \"status\": [\"ASSIGNED\"] } ] }"),
            Arguments.of("filter has no status", "{ \"TASK\": [ {} ] }"),
            Arguments.of("status is not a valid enum", "{ \"TASK\": [ { \"status\": [\"NOT_A_STATUS\"] } ] }"),
            Arguments.of(
                "duplicate status filter",
                """
                {
                  "TASK": [ { "status": ["CREATED"] }, { "status": ["CREATED"] } ]
                }"""
            ),
            Arguments.of(
                "filter has multiple statuses",
                "{ \"TASK\": [ { \"status\": [\"ASSIGNED\", \"SUSPENDED\"] } ] }"
            )
        );
    }
}
