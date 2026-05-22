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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.List;
import java.util.Map;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.rest.payload.CloudRuntimeEntitySort;
import org.activiti.cloud.services.query.util.ProcessInstanceSearchRequestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Sort;
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
@WithMockUser(username = AbstractProcessInstanceEntitySearchControllerIT.USER, roles = "ACTIVITI_ADMIN")
class ProcessInstanceEntitySearchAdminControllerIT extends AbstractProcessInstanceEntitySearchControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
        .waitingFor(Wait.forListeningPort());

    @Override
    protected String getSearchEndpoint() {
        return "/admin/v1/process-instances/search";
    }

    @Override
    protected String getCountEndpoint() {
        return "/admin/v1/process-instances/count";
    }

    @Test
    void should_return_OnlyMainProcesses_WhenIncludeSubProcessIsFalse() {
        ProcessInstanceEntity processInstance1 = queryTestUtils.buildProcessInstance().buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
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
    void should_return_AllProcessInstancesWithoutItSelfAsASubprocess() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withName("root-process")
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH, hasItem(List.of()));
    }

    @Test
    void should_return_AllProcessInstancesWithoutSubProcess() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .subprocessOf(processInstance1)
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH, hasItem(List.of()))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH, hasItem(List.of(Map.of("id", processInstance2.getId()))));
    }

    @Test
    void should_return_AllProcessInstances() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("another-user")
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_return_SubProcesses() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("hruser")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("hruser")
            .subprocessOf(processInstance1)
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .get("/admin/v1/process-instances/" + processInstance1.getId() + "/subprocesses")
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()));
    }

    @Test
    void should_return_AllProcessInstancesWithSubProcess() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .subprocessOf(processInstance1)
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance2.getId()))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH, hasItem(List.of()))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH, hasItem(List.of(Map.of("id", processInstance2.getId()))));
    }

    @Test
    void should_return_ProcessInstancesWithSubSubProcesses() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withName("root-process")
            .buildAndSave();
        ProcessInstanceEntity subProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("sub-process")
            .subprocessOf(processInstance1)
            .buildAndSave();

        ProcessInstanceEntity subSubProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withInitiator(USER)
            .withName("sub-sub-process")
            .subprocessOf(subProcessInstance)
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withIncludeSubprocesses(false)
            .withIncludeUnlinkedProcesses(false)
            .withIncludeLinkedProcesses(false);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(subProcessInstance.getId())))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(subSubProcessInstance.getId())))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH + "[0]", hasItem(Map.of("id", subProcessInstance.getId())))
            .body(PROCESS_INSTANCE_SUBPROCESS_JSON_PATH + "[0]", hasItem(Map.of("id", subSubProcessInstance.getId())));
    }

    @Test
    void should_returnProcessInstances_filteredByInitiator() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user1")
            .buildAndSave();
        ProcessInstanceEntity processInstance2 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("user2")
            .buildAndSave();
        queryTestUtils.buildProcessInstance().withInitiator("user3").buildAndSave();

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
            .body(equalTo("2"));
    }

    @Test
    void should_return_LinkedProcesses() {
        ProcessInstanceEntity processInstance1 = queryTestUtils
            .buildProcessInstance()
            .withInitiator("hruser")
            .buildAndSave();
        ProcessInstanceEntity linkedProcess = queryTestUtils
            .buildProcessInstance()
            .withInitiator("hruser")
            .withLinkedProcessInstanceType("task-form")
            .withLinkedProcessInstanceId(processInstance1.getId())
            .buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .get("/admin/v1/process-instances/" + processInstance1.getId() + "/linkedprocesses")
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(linkedProcess.getId()));
    }

    @Test
    void should_return_AllProcessInstancesWithLinkedProcesses() {
        ProcessInstanceEntity rootProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        for (int i = 1; i <= 18; i++) {
            queryTestUtils
                .buildProcessInstance()
                .withName("linked-process")
                .withInitiator(USER)
                .withLinkedProcessInstanceId(rootProcessInstance.getId())
                .withLinkedProcessInstanceType("form-type")
                .buildAndSave();
        }

        var linkedProcesses = queryTestUtils
            .buildProcessInstance()
            .findProcessInstanceByFilter(
                new ProcessInstanceSearchRequestBuilder()
                    .withLinkedProcessInstanceId(rootProcessInstance.getId())
                    .build()
            );

        var response = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);

        assertThat(response.body().jsonPath().getList(PROCESS_INSTANCES_JSON_PATH)).hasSize(19);

        assertThat(response.body().jsonPath().getList(PROCESS_INSTANCE_IDS_JSON_PATH))
            .contains(rootProcessInstance.getId());

        for (ProcessInstanceEntity linkedProcessInstance : linkedProcesses) {
            assertThat(response.body().jsonPath().getList(PROCESS_INSTANCE_IDS_JSON_PATH))
                .contains(linkedProcessInstance.getId());
            assertThat(response.body().jsonPath().getList(linkedProcessesPath("root-process")))
                .contains(Map.of("id", linkedProcessInstance.getId()));
            assertThat(response.body().jsonPath().getList(linkedProcessesPath(linkedProcessInstance.getName())))
                .isEmpty();
        }
    }

    @Test
    void should_return_PaginatedProcessInstancesWithAllLinkedProcesses() {
        ProcessInstanceEntity rootProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        for (int i = 1; i <= 18; i++) {
            queryTestUtils
                .buildProcessInstance()
                .withName("linked-process")
                .withInitiator(USER)
                .withLinkedProcessInstanceId(rootProcessInstance.getId())
                .withLinkedProcessInstanceType("form-type")
                .buildAndSave();
        }

        var linkedProcesses = queryTestUtils
            .buildProcessInstance()
            .findProcessInstanceByFilter(
                new ProcessInstanceSearchRequestBuilder()
                    .withLinkedProcessInstanceId(rootProcessInstance.getId())
                    .build()
            );

        var response = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                "{\"includeSubprocesses\":false,\"includeUnlinkedProcesses\":true,\"sort\":{\"field\":\"name\",\"direction\":\"asc\",\"isProcessVariable\":false}}"
            )
            .when()
            .post("/admin/v1/process-instances/search?maxItems=15&skipCount=15")
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);

        assertThat(response.body().jsonPath().getList(PROCESS_INSTANCES_JSON_PATH)).hasSize(4);

        assertThat(response.body().jsonPath().getList(PROCESS_INSTANCE_IDS_JSON_PATH))
            .contains(rootProcessInstance.getId());

        for (ProcessInstanceEntity linkedProcessInstance : linkedProcesses) {
            assertThat(response.body().jsonPath().getList(linkedProcessesPath("root-process")))
                .contains(Map.of("id", linkedProcessInstance.getId()));
        }
    }

    @Test
    void should_return_AllRelatedToProcessInstancesForASpecificProcess() {
        ProcessInstanceEntity rootProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        ProcessInstanceEntity subProcessInstance = queryTestUtils
            .buildProcessInstance()
            .subprocessOf(rootProcessInstance)
            .withName("sub-process")
            .withInitiator(USER)
            .buildAndSave();

        ProcessInstanceEntity subSubProcessInstance = queryTestUtils
            .buildProcessInstance()
            .subprocessOf(subProcessInstance)
            .withName("sub-sub-process")
            .withInitiator(USER)
            .buildAndSave();

        ProcessInstanceEntity linkedProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process")
            .withLinkedProcessInstanceId(rootProcessInstance.getId())
            .withLinkedProcessInstanceType("form-type")
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessRelatedTo(rootProcessInstance.getId());

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(4))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(rootProcessInstance.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(subProcessInstance.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(subSubProcessInstance.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(linkedProcessInstance.getId()));
    }

    @Test
    void should_return_AllRelatedToProcessInstancesForASpecificProcess_sortedByProcessType() {
        ProcessInstanceEntity rootProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withId("root-process-id")
            .withName("root-process")
            .withInitiator(USER)
            .withParentId("root-process-id")
            .buildAndSave();

        ProcessInstanceEntity subProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("sub-process")
            .withInitiator(USER)
            .subprocessOf(rootProcessInstance)
            .buildAndSave();

        ProcessInstanceEntity linkedProcessInstance = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process")
            .withLinkedProcessInstanceId(rootProcessInstance.getId())
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withProcessRelatedTo(rootProcessInstance.getId())
            .withSort(new CloudRuntimeEntitySort("type", Sort.Direction.ASC, false, null, null));

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
                contains(subProcessInstance.getId(), rootProcessInstance.getId(), linkedProcessInstance.getId())
            )
            .body("_embedded.processInstances.type", contains("call-activity", "main-process", "task-form"));

        requestBuilder.invertSort();

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
                contains(linkedProcessInstance.getId(), rootProcessInstance.getId(), subProcessInstance.getId())
            )
            .body("_embedded.processInstances.type", contains("task-form", "main-process", "call-activity"));
    }

    @Test
    void should_return_UnlinkedProcessInstances() {
        var mainProcess = queryTestUtils
            .buildProcessInstance()
            .withName("main-process")
            .withInitiator(USER)
            .buildAndSave();

        var orphanProcess = queryTestUtils
            .buildProcessInstance()
            .withName("orphan-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        var linkedProcess = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .withLinkedProcessInstanceId(mainProcess.getId())
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withIncludeUnlinkedProcesses(true)
            .withIncludeLinkedProcesses(false);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(mainProcess.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(orphanProcess.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(linkedProcess.getId())));
    }

    @Test
    void should_not_return_UnlinkedProcessInstances() {
        var mainProcess = queryTestUtils
            .buildProcessInstance()
            .withName("main-process")
            .withInitiator(USER)
            .buildAndSave();

        var orphanProcess = queryTestUtils
            .buildProcessInstance()
            .withName("orphan-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        var linkedProcess = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .withLinkedProcessInstanceId(mainProcess.getId())
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withIncludeUnlinkedProcesses(false)
            .withIncludeLinkedProcesses(false);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(mainProcess.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(orphanProcess.getId())))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(linkedProcess.getId())));
    }

    @Test
    void should_return_LinkedProcessInstances() {
        var mainProcess = queryTestUtils
            .buildProcessInstance()
            .withName("main-process")
            .withInitiator(USER)
            .buildAndSave();

        var orphanProcess = queryTestUtils
            .buildProcessInstance()
            .withName("orphan-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        var linkedProcess = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .withLinkedProcessInstanceId(mainProcess.getId())
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withIncludeLinkedProcesses(true)
            .withIncludeUnlinkedProcesses(true);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(3))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(mainProcess.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(orphanProcess.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(linkedProcess.getId()));
    }

    @Test
    void should_not_return_LinkedProcessInstances() {
        var mainProcess = queryTestUtils
            .buildProcessInstance()
            .withName("main-process")
            .withInitiator(USER)
            .buildAndSave();

        var orphanProcess = queryTestUtils
            .buildProcessInstance()
            .withName("orphan-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        var linkedProcess = queryTestUtils
            .buildProcessInstance()
            .withName("linked-process")
            .withInitiator(USER)
            .withLinkedProcessInstanceType("task-form")
            .withLinkedProcessInstanceId(mainProcess.getId())
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withIncludeLinkedProcesses(false)
            .withIncludeUnlinkedProcesses(true);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(2))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(mainProcess.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(orphanProcess.getId()))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, not(hasItem(linkedProcess.getId())));
    }

    @Test
    void should_return_LinkedProcessesOfSubprocess_inRootLinkedProcessesList() {
        ProcessInstanceEntity rootProcess = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        ProcessInstanceEntity subProcess = queryTestUtils
            .buildProcessInstance()
            .withName("sub-process")
            .withInitiator(USER)
            .subprocessOf(rootProcess)
            .buildAndSave();

        ProcessInstanceEntity linkedToSub = queryTestUtils
            .buildProcessInstance()
            .withName("linked-to-sub")
            .withInitiator(USER)
            .withLinkedProcessInstanceId(subProcess.getId())
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        var response = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getList(linkedProcessesPath("root-process")))
            .contains(Map.of("id", linkedToSub.getId()));
        assertThat(response.body().jsonPath().getList(linkedProcessesPath("linked-to-sub"))).isEmpty();
    }

    @Test
    void should_mergeLinkedProcessesOfRootAndSubprocess_inRootLinkedProcessesList() {
        ProcessInstanceEntity rootProcess = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        ProcessInstanceEntity subProcess = queryTestUtils
            .buildProcessInstance()
            .withName("sub-process")
            .withInitiator(USER)
            .subprocessOf(rootProcess)
            .buildAndSave();

        ProcessInstanceEntity linkedToRoot = queryTestUtils
            .buildProcessInstance()
            .withName("linked-to-root")
            .withInitiator(USER)
            .withLinkedProcessInstanceId(rootProcess.getId())
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        ProcessInstanceEntity linkedToSub = queryTestUtils
            .buildProcessInstance()
            .withName("linked-to-sub")
            .withInitiator(USER)
            .withLinkedProcessInstanceId(subProcess.getId())
            .withLinkedProcessInstanceType("task-form")
            .buildAndSave();

        var response = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post(getSearchEndpoint())
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().jsonPath().getList(linkedProcessesPath("root-process")))
            .contains(Map.of("id", linkedToRoot.getId()))
            .contains(Map.of("id", linkedToSub.getId()));
    }

    @Test
    void should_return_EmptyLinkedProcesses_whenSubprocessHasNoLinkedProcesses() {
        ProcessInstanceEntity rootProcess = queryTestUtils
            .buildProcessInstance()
            .withName("root-process")
            .withInitiator(USER)
            .buildAndSave();

        queryTestUtils
            .buildProcessInstance()
            .withName("sub-process")
            .withInitiator(USER)
            .subprocessOf(rootProcess)
            .buildAndSave();

        ProcessInstanceSearchRequestBuilder requestBuilder = new ProcessInstanceSearchRequestBuilder()
            .withIncludeSubprocesses(false)
            .withIncludeLinkedProcesses(false);

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBuilder.buildJson())
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(rootProcess.getId()))
            .body(linkedProcessesPath("root-process"), empty());
    }
}
