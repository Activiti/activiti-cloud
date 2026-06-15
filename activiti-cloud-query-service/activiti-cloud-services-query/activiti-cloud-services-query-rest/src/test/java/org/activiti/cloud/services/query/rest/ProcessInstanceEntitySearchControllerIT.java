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
import static org.activiti.cloud.services.query.util.QueryTestUtils.linkedProcessesPath;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.util.List;
import java.util.Map;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.common.feature.FeatureToggleHolder;
import org.activiti.cloud.services.query.QueryFeatureToggles;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@WithMockUser(username = AbstractProcessInstanceEntitySearchControllerIT.USER, roles = "ACTIVITI_USER")
class ProcessInstanceEntitySearchControllerIT extends AbstractProcessInstanceEntitySearchControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .waitingFor(Wait.forListeningPort());

    @Override
    protected String getSearchEndpoint() {
        return "/v1/process-instances/search";
    }

    @Override
    protected String getCountEndpoint() {
        return "/v1/process-instances/count";
    }

    @AfterEach
    void resetFeatureToggle() {
        FeatureToggleHolder.reset();
    }

    @ParameterizedTest(name = "existsSubqueries={0}")
    @ValueSource(booleans = { false, true })
    void should_return_RestrictedProcessInstances(boolean existsSubqueriesEnabled) {
        if (existsSubqueriesEnabled) {
            FeatureToggleHolder.initialize(QueryFeatureToggles.FEATURE_EXISTS_SUBQUERIES::equals);
        } else {
            FeatureToggleHolder.reset();
        }

        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("anotheruser").buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body("_embedded.processInstances[0].id", equalTo(processInstance1.getId()));

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getCountEndpoint())
            .then()
            .statusCode(200)
            .body(IsEqual.equalTo("1"));
    }

    @Test
    void should_returnProcessInstances_withLinkedProcessInstances() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user1")
            .withLinkedProcessInstanceId("123-lin-ked-111")
            .withLinkedProcessInstanceType("my-type")
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers(USER))
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withInitiator("user1")
            .withLinkedProcessInstanceId("123-lin-ked-222")
            .withLinkedProcessInstanceType("my-type")
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers(USER))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withLinkedProcessInstanceId("123-lin-ked-111")
            .withLinkedProcessInstanceType("my-type");

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(
                "_embedded.processInstances[0].linkedProcessInstanceId",
                equalTo(processInstance1.getLinkedProcessInstanceId())
            )
            .body(
                "_embedded.processInstances[0].linkedProcessInstanceType",
                equalTo(processInstance1.getLinkedProcessInstanceType())
            );
    }

    @Test
    void should_returnProcessInstances_filteredByInitiator() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user1")
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers(USER))
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user2")
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers(USER))
            .buildAndSave();
        queryTestUtils
            .buildProcessInstance()
            .withInitiator("user3")
            .withTasks(queryTestUtils.buildTask().withTaskCandidateUsers(USER))
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withInitiators("user1", "user2");

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

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getCountEndpoint())
            .then()
            .statusCode(200)
            .body(IsEqual.equalTo("2"));
    }

    @Test
    void should_return_OnlyMainProcesses_WhenIncludeSubProcessIsFalse() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .subprocessOf(processInstance1)
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"includeSubprocesses\": false}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH, hasItem(List.of(Map.of("id", processInstance2.getId()))));
    }

    @Test
    void should_return_AllProcessInstancesWithLinkedProcesses() {
        ProcessInstanceEntity rootProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        ProcessInstanceEntity linkedProcessInstance1 = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process-1")
            .withInitiator(USER)
            .withLinkedProcessInstanceId(rootProcessInstance.getId())
            .buildAndSave();
        ProcessInstanceEntity linkedProcessInstance2 = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process-2")
            .withLinkedProcessInstanceId(rootProcessInstance.getId())
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(rootProcessInstance.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(linkedProcessInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(linkedProcessInstance2.getId())))
            .body(linkedProcessesPath("linked-process-1"), hasSize(0))
            .body(
                linkedProcessesPath("root-process"),
                containsInAnyOrder(Map.of("id", linkedProcessInstance1.getId()))
            );
    }

    @Test
    void should_return_processInstancesWithEmptyLinkedProcesses() {
        ProcessInstanceEntity rootProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        ProcessInstanceEntity linkedProcessInstance2 = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process-2")
            .withLinkedProcessInstanceId(rootProcessInstance.getId())
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(rootProcessInstance.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(linkedProcessInstance2.getId())))
            .body(linkedProcessesPath("root-process"), hasSize(0));
    }
}
