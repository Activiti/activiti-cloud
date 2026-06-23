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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Date;
import java.util.UUID;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessVariableHistoryRepository;
import org.activiti.cloud.services.query.model.ProcessVariableHistoryEntity;
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
    properties = { "spring.main.banner-mode=off", "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false" }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
@WithMockUser(roles = "ACTIVITI_ADMIN")
class ProcessInstanceVariableHistoryAdminControllerIT {

    private static final String ENTRIES_ROOT = "list.entries";
    private static final String VARIABLE_NAME_PATH = ENTRIES_ROOT + ".entry.variableName";
    private static final String VALUE_PATH = ENTRIES_ROOT + ".entry.value";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine").waitingFor(
        Wait.forListeningPort()
    );

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProcessVariableHistoryRepository historyRepository;

    @Autowired
    private QueryTestUtils queryTestUtils;

    @BeforeEach
    void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
    }

    @AfterEach
    void cleanUp() {
        historyRepository.deleteAll();
        queryTestUtils.cleanUp();
    }

    @Test
    void should_returnHistory_when_dataExistsForProcessInstance() {
        var processInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, "myVar", "hello", new Date(1000), 1));
        historyRepository.save(buildHistoryEntity(processInstanceId, "otherVar", "world", new Date(2000), 1));

        given()
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get("/admin/v1/process-instances/{processInstanceId}/variables/history", processInstanceId)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(2))
            .body(VARIABLE_NAME_PATH, contains("myVar", "otherVar"))
            .body(VALUE_PATH, contains("hello", "world"));
    }

    @Test
    void should_returnHistory_when_calledWithHalJson() {
        var processInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, "myVar", "hello", new Date(1000), 1));

        given()
            .accept("application/hal+json")
            .when()
            .get("/admin/v1/process-instances/{processInstanceId}/variables/history", processInstanceId)
            .then()
            .statusCode(200)
            .body("_embedded", notNullValue())
            .body("_links", notNullValue());
    }

    @Test
    void should_returnEmptyList_when_noHistoryExistsForProcessInstance() {
        var processInstanceId = UUID.randomUUID().toString();

        given()
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get("/admin/v1/process-instances/{processInstanceId}/variables/history", processInstanceId)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(0));
    }

    @Test
    void should_returnOnlyHistoryForRequestedProcessInstance() {
        var processInstanceId = UUID.randomUUID().toString();
        var otherProcessInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, "myVar", "hello", new Date(1000), 1));
        historyRepository.save(buildHistoryEntity(otherProcessInstanceId, "otherVar", "world", new Date(1000), 1));

        given()
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get("/admin/v1/process-instances/{processInstanceId}/variables/history", processInstanceId)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(1))
            .body(VARIABLE_NAME_PATH, contains("myVar"));
    }

    @Test
    void should_returnHistoryOrderedByEventTimeAndSequenceNumber() {
        var processInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, "var1", "v1", new Date(2000), 1));
        historyRepository.save(buildHistoryEntity(processInstanceId, "var2", "v2", new Date(1000), 1));
        historyRepository.save(buildHistoryEntity(processInstanceId, "var3", "v3", new Date(1000), 2));

        given()
            .accept(MediaType.APPLICATION_JSON)
            .when()
            .get("/admin/v1/process-instances/{processInstanceId}/variables/history", processInstanceId)
            .then()
            .statusCode(200)
            .body(ENTRIES_ROOT, hasSize(3))
            .body(VARIABLE_NAME_PATH, contains("var2", "var3", "var1"));
    }

    @Test
    void should_returnPaginationMetadata() {
        var processInstanceId = UUID.randomUUID().toString();
        historyRepository.save(buildHistoryEntity(processInstanceId, "var1", "val1", new Date(1000), 1));
        historyRepository.save(buildHistoryEntity(processInstanceId, "var2", "val2", new Date(2000), 1));
        historyRepository.save(buildHistoryEntity(processInstanceId, "var3", "val3", new Date(3000), 1));

        given()
            .accept(MediaType.APPLICATION_JSON)
            .param("maxItems", 2)
            .param("skipCount", 0)
            .when()
            .get("/admin/v1/process-instances/{processInstanceId}/variables/history", processInstanceId)
            .then()
            .statusCode(200)
            .body("list.pagination.totalItems", equalTo(3))
            .body("list.pagination.count", equalTo(2));
    }

    private ProcessVariableHistoryEntity buildHistoryEntity(
        String processInstanceId,
        String variableName,
        String value,
        Date eventTime,
        int sequenceNumber
    ) {
        var entity = new ProcessVariableHistoryEntity();
        entity.setProcessInstanceId(processInstanceId);
        entity.setVariableName(variableName);
        entity.setType(String.class.getName());
        entity.setValue(value);
        entity.setDeleted(false);
        entity.setEventTime(eventTime);
        entity.setRecordCreateTime(new Date(0L));
        entity.setSequenceNumber(sequenceNumber);
        return entity;
    }
}
