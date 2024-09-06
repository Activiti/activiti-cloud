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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.activiti.QueryRestTestApplication;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.app.repository.VariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.ProcessVariableEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.jetbrains.annotations.NotNull;
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
//TODO Make the test work using AlfrescoJackson2HttpMessageConverter to simulate the actual response that we have in real environment
public class TaskAdminControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    TaskControllerHelper taskControllerHelper;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskVariableRepository taskVariableRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private VariableRepository variableRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeEach
    public void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testadmin")
    void should_returnTasks_withOnlyRequestedProcessVariables_whenSearchingByTaskVariableNameAndValue() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableEntity> processVariables = createProcessVariables(processInstanceEntity);

        TaskVariableEntity taskVariable1 = createTaskVariable();
        TaskVariableEntity taskVariable2 = createTaskVariable();

        Set<TaskVariableEntity> taskVariables = new HashSet<>();
        taskVariables.add(taskVariable1);
        taskVariables.add(taskVariable2);

        createTaskWithVariables(processInstanceEntity, taskVariables, processVariables);

        processInstanceRepository.save(processInstanceEntity);

        ProcessVariableEntity variableToFetch = processVariables.stream().findFirst().get();

        given()
            .webAppContextSetup(context)
            .accept("application/hal+json;charset=UTF-8")
            .when()
            .get(
                "/admin/v1/tasks?maxItems=500&skipCount=0&sort=createdDate,desc&variableKeys=" +
                processInstanceEntity.getProcessDefinitionKey() +
                "/" +
                variableToFetch.getName() +
                "&variables.name=" +
                taskVariable1.getName() +
                "&variables.value=" +
                taskVariable1.getValue()
            )
            .then()
            .statusCode(200)
            .body("_embedded.tasks", hasSize(1));
    }

    @Test
    @WithMockUser(username = "testadmin")
    void should_parseTaskSearchRequest() {
        String taskSearchRequest =
            """
            {
                "completedFrom": "2021-01-01T00:00:00Z",
                "completedTo": "2021-01-01T00:00:00Z",
                "candidateUserId": ["candidateUserId"],
                "candidateGroupId": ["candidateGroupId"],
                 "processVariableFilters": [
                    {
                        "processDefinitionKey": "processDefinitionKey",
                        "name": "name",
                        "type": "string",
                        "value": "value",
                        "operator": "eq"
                    }
                ],
                "taskVariableFilters": [
                    {
                        "name": "name",
                        "type": "string",
                        "value": "value",
                        "operator": "eq"
                    }
                ],
                "processVariableKeys": ["processDef/varName"],
                "onlyStandalone": true,
                "onlyRoot": true,
                "assignee": ["assignee"],
                "name": ["name"],
                "description": ["description"],
                "priority": [1],
                "status": ["CREATED"],
                "completedBy": ["completedBy"]
            }
            """;

        given()
            .webAppContextSetup(context)
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequest)
            .when()
            .post("/admin/v1/tasks/search")
            .then()
            .statusCode(200);
    }

    @Test
    @WithMockUser(username = "testadmin")
    void should_filterByProcessVariablesAndTaskVariables() {
        ProcessInstanceEntity process1 = createProcessInstance();
        ProcessInstanceEntity process2 = createProcessInstance();
        Set<ProcessVariableEntity> processVars1 = createProcessVariables(process1);
        Set<ProcessVariableEntity> processVars2 = createProcessVariables(process2);

        TaskVariableEntity taskVariable1 = createTaskVariable();
        TaskVariableEntity taskVariable2 = createTaskVariable();
        taskVariableRepository.save(taskVariable1);
        taskVariableRepository.save(taskVariable2);

        createTaskWithVariables(process1, Set.of(taskVariable1), processVars1);
        createTaskWithVariables(process2, Set.of(taskVariable2), processVars2);

        processInstanceRepository.save(process1);
        processInstanceRepository.save(process2);

        String taskSearchRequest = String.format(
            """
            {
                "processVariableFilters": [
                    {
                        "processDefinitionKey": "%s",
                        "name": "%s",
                        "type": "string",
                        "value": "%s",
                        "operator": "eq"
                    }
                ],
                "taskVariableFilters": [
                    {
                        "name": "%s",
                        "type": "string",
                        "value": "%s",
                        "operator": "eq"
                    }
                ]
            }
            """,
            taskVariable1.getProcessInstance().getProcessDefinitionKey(),
            processVars1.stream().findFirst().get().getName(),
            processVars1.stream().findFirst().get().getValue(),
            taskVariable1.getName(),
            taskVariable1.getValue()
        );

        given()
            .webAppContextSetup(context)
            .contentType(MediaType.APPLICATION_JSON)
            .body(taskSearchRequest)
            .when()
            .post("/admin/v1/tasks/search")
            .then()
            .statusCode(200)
            .body("_embedded.tasks", hasSize(1))
            .body("_embedded.tasks[0].id", equalTo(taskVariable1.getTask().getId()));
    }

    @NotNull
    private Set<ProcessVariableEntity> createProcessVariables(ProcessInstanceEntity processInstanceEntity) {
        Set<ProcessVariableEntity> variables = new HashSet<>();

        for (int i = 0; i < 2; i++) {
            ProcessVariableEntity processVariableEntity = new ProcessVariableEntity();
            processVariableEntity.setName("name" + i);
            processVariableEntity.setValue(UUID.randomUUID().toString());
            processVariableEntity.setType("string");
            processVariableEntity.setProcessInstanceId(processInstanceEntity.getId());
            processVariableEntity.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
            processVariableEntity.setProcessInstance(processInstanceEntity);
            variables.add(processVariableEntity);
        }
        variableRepository.saveAll(variables);
        processInstanceEntity.setVariables(variables);
        processInstanceRepository.save(processInstanceEntity);
        return variables;
    }

    @NotNull
    private TaskVariableEntity createTaskVariable() {
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity();
        taskVariableEntity.setName("name" + UUID.randomUUID());
        taskVariableEntity.setType("string");
        taskVariableEntity.setValue(UUID.randomUUID().toString());
        taskVariableRepository.save(taskVariableEntity);
        return taskVariableEntity;
    }

    private void createTaskWithVariables(
        ProcessInstanceEntity processInstanceEntity,
        Set<TaskVariableEntity> taskVariables,
        Set<ProcessVariableEntity> processVariables
    ) {
        TaskEntity taskEntity = new TaskEntity();
        String taskId = UUID.randomUUID().toString();
        taskEntity.setId(taskId);
        taskEntity.setCreatedDate(new Date());
        taskEntity.setProcessInstance(processInstanceEntity);
        taskEntity.setProcessInstanceId(processInstanceEntity.getId());
        taskEntity.setVariables(taskVariables);
        taskEntity.setProcessVariables(processVariables);
        taskRepository.save(taskEntity);
        taskVariables.forEach(taskVariableEntity -> {
            taskVariableEntity.setTaskId(taskId);
            taskVariableEntity.setTask(taskEntity);
            taskVariableEntity.setProcessInstance(processInstanceEntity);
            taskVariableRepository.save(taskVariableEntity);
        });
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        String process = UUID.randomUUID().toString();
        processInstanceEntity.setId(process);
        processInstanceEntity.setName(process);
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName(process);
        processInstanceEntity.setProcessDefinitionKey(process);
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }
}
