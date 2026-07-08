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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.rest.filter.VariableType;
import org.activiti.cloud.services.query.util.QueryTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
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
@WithMockUser(username = AbstractTaskControllerIT.CURRENT_USER, roles = "ACTIVITI_ADMIN")
class TaskAdminControllerIT extends AbstractTaskControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Override
    protected String getSearchEndpointHttpGet() {
        return "/admin/v1/tasks";
    }

    @Override
    protected String getSearchEndpointHttpPost() {
        return "/admin/v1/tasks/search";
    }

    @Override
    protected String getCountEndpointHttpPost() {
        return "/admin/v1/tasks/count";
    }

    @Test
    void should_returnTasksAndCount_unrestrictedTasks() {
        String otherUser = "other-user";

        TaskEntity task1 = queryTestUtils.buildTask().withOwner(otherUser).buildAndSave();

        TaskEntity task2 = queryTestUtils.buildTask().withTaskCandidateUsers(otherUser).buildAndSave();

        TaskEntity task3 = queryTestUtils.buildTask().withAssignee(otherUser).buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpointHttpPost())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(3))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(task1.getId(), task2.getId(), task3.getId()));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post("/admin/v1/tasks/count")
            .then()
            .statusCode(200)
            .body(equalTo("3"));
    }

    @Test
    void should_returnTasks_whenPostingToAdminTasksEndpointWithoutVariableKeys() {
        TaskEntity task1 = queryTestUtils.buildTask().buildAndSave();
        TaskEntity task2 = queryTestUtils.buildTask().buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpointHttpGet())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(2))
            .body(TASK_IDS_JSON_PATH, containsInAnyOrder(task1.getId(), task2.getId()));
    }

    @Test
    void should_returnTasksWithProcessVariables_whenPostingToAdminTasksEndpointWithVariableKeys() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"variableKeys\": [\"" + PROCESS_DEFINITION_KEY + "/" + VAR_NAME + "\"]}")
            .when()
            .post(getSearchEndpointHttpGet())
            .then()
            .statusCode(200)
            .body(TASKS_JSON_PATH, hasSize(1))
            .body(TASKS_JSON_PATH + "[0].processVariables", hasSize(1))
            .body(TASKS_JSON_PATH + "[0].processVariables[0].name", is(VAR_NAME));
    }

    @Test
    void should_deleteAllTasks_whenDeletingAdminTasksEndpoint() {
        queryTestUtils
            .buildProcessInstance()
            .withProcessDefinitionKey(PROCESS_DEFINITION_KEY)
            .withVariables(new QueryTestUtils.VariableInput(VAR_NAME, VariableType.STRING, "value1"))
            .withTasks(queryTestUtils.buildTask())
            .buildAndSave();
        queryTestUtils.buildTask().buildAndSave();

        given().when().delete(getSearchEndpointHttpGet()).then().statusCode(200);
    }
}
