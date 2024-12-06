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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Override
    protected String getSearchEndpointHttpGet() {
        return "/admin/v1/tasks";
    }

    @Override
    protected String getSearchEndpointHttpPost() {
        return "/admin/v1/tasks/search";
    }

    @Test
    void should_returnTasks_unrestrictedTasks() {
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
    }
}
