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
import static org.activiti.cloud.services.query.rest.TaskControllerIT.CURRENT_USER;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@WithMockUser(username = CURRENT_USER, roles = "ACTIVITI_ADMIN")
class ServiceTaskAdminControllerIT {

    @Autowired
    private QueryTestUtils queryTestUtils;

    @Autowired
    private WebApplicationContext context;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final String SEARCH_ENDPOINT = "/admin/v1/service-tasks";
    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final String ENTRIES_ROOT = "list.entries";
    private static final String SERVICE_TASKS_ID_ROOT = ENTRIES_ROOT + ".entry.id";

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
    void should_returnServiceTasks_filteredByStaredDateFrom() {
        queryTestUtils.buildServiceTask().withId("1").withStartedDate(new Date(1000)).save();

        queryTestUtils.buildServiceTask().withId("2").withStartedDate(new Date(2000)).save();

        queryTestUtils.buildServiceTask().withStartedDate(new Date(500)).save();

        given()
            .param("startedFrom", dateTimeFormatter.format(new Date(1000)))
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get(SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(2))
            .body(SERVICE_TASKS_ID_ROOT, containsInAnyOrder("1", "2"));
    }

    @Test
    void should_returnServiceTasks_filteredByStaredDateTo() {
        queryTestUtils.buildServiceTask().withId("1").withStartedDate(new Date(1000)).save();

        queryTestUtils.buildServiceTask().withId("2").withStartedDate(new Date(2000)).save();

        queryTestUtils.buildServiceTask().withStartedDate(new Date(3000)).save();

        given()
            .param("startedTo", dateTimeFormatter.format(new Date(2000)))
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get(SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(2))
            .body(SERVICE_TASKS_ID_ROOT, containsInAnyOrder("1", "2"));
    }

    @Test
    void should_returnServiceTasks_filteredByCompletedDateFrom() {
        queryTestUtils.buildServiceTask().withId("1").withCompletedDate(new Date(1000)).save();

        queryTestUtils.buildServiceTask().withId("2").withCompletedDate(new Date(2000)).save();

        queryTestUtils.buildServiceTask().withCompletedDate(new Date(500)).save();

        given()
            .param("completedFrom", dateTimeFormatter.format(new Date(1000)))
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get(SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(2))
            .body(SERVICE_TASKS_ID_ROOT, containsInAnyOrder("1", "2"));
    }

    @Test
    void should_returnServiceTasks_filteredByCompletedDateTo() {
        queryTestUtils.buildServiceTask().withId("1").withCompletedDate(new Date(1000)).save();

        queryTestUtils.buildServiceTask().withId("2").withCompletedDate(new Date(2000)).save();

        queryTestUtils.buildServiceTask().withCompletedDate(new Date(3000)).save();

        given()
            .param("completedTo", dateTimeFormatter.format(new Date(2000)))
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get(SEARCH_ENDPOINT)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(2))
            .body(SERVICE_TASKS_ID_ROOT, containsInAnyOrder("1", "2"));
    }
}
