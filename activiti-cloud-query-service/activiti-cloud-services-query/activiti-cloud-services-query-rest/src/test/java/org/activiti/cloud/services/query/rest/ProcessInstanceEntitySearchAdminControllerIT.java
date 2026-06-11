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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

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

    private static final String SUBPROCESSES_COUNT_PATH = "_embedded.processInstances.subprocessesCount";
    private static final String LINKED_PROCESSES_COUNT_PATH = "_embedded.processInstances.linkedProcessesCount";

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
        queryTestUtils.buildProcessInstance().subprocessOf(processInstance1).buildAndSave();

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"includeSubprocesses\": false}")
            .when()
            .post(getSearchEndpoint())
            .then()
            .statusCode(200)
            .body(PROCESS_INSTANCES_JSON_PATH, hasSize(1))
            .body(PROCESS_INSTANCE_IDS_JSON_PATH, hasItem(processInstance1.getId()))
            .body(SUBPROCESSES_COUNT_PATH, hasItem(1));
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
            .body(SUBPROCESSES_COUNT_PATH, hasItem(0));
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
            .body(SUBPROCESSES_COUNT_PATH, hasSize(2))
            .body(SUBPROCESSES_COUNT_PATH, hasItem(0))
            .body(SUBPROCESSES_COUNT_PATH, hasItem(1));
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
            .body(SUBPROCESSES_COUNT_PATH, hasItem(0))
            .body(SUBPROCESSES_COUNT_PATH, hasItem(1));
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
            .body(SUBPROCESSES_COUNT_PATH, hasItem(2));
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

        Integer rootLinkedCount = response
            .body()
            .jsonPath()
            .getList(
                PROCESS_INSTANCES_JSON_PATH +
                ".findAll { it.id == '" +
                rootProcessInstance.getId() +
                "' }.linkedProcessesCount",
                Integer.class
            )
            .get(0);
        assertThat(rootLinkedCount).isEqualTo(18);

        assertThat(
            response
                .body()
                .jsonPath()
                .getList(
                    PROCESS_INSTANCES_JSON_PATH +
                    ".findAll { it.id != '" +
                    rootProcessInstance.getId() +
                    "' }.linkedProcessesCount",
                    Integer.class
                )
        )
            .containsOnly(0);
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

        Integer rootLinkedCount = response
            .body()
            .jsonPath()
            .getList(
                PROCESS_INSTANCES_JSON_PATH +
                ".findAll { it.id == '" +
                rootProcessInstance.getId() +
                "' }.linkedProcessesCount",
                Integer.class
            )
            .get(0);
        assertThat(rootLinkedCount).isEqualTo(18);
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
}
