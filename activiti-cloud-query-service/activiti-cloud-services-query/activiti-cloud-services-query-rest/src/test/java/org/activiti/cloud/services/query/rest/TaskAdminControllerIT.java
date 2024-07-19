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
import static io.restassured.module.mockmvc.RestAssuredMockMvc.webAppContextSetup;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
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
import org.activiti.cloud.services.query.model.ProcessVariableInstance;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = { QueryRestTestApplication.class, AlfrescoWebAutoConfiguration.class },
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "logging.level.org.hibernate.collection.spi=warn",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true",
    }
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
        taskRepository.deleteAll();
        taskVariableRepository.deleteAll();
        processInstanceRepository.deleteAll();
        variableRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testadmin", roles = "ACTIVITI_ADMIN")
    void should_returnTasks_withOnlyRequestedProcessVariables_whenSearchingByTaskVariableNameAndValue() {
        ProcessInstanceEntity processInstanceEntity = createProcessInstance();
        Set<ProcessVariableInstance> processVariables = createProcessVariables(processInstanceEntity);

        TaskVariableEntity taskVariable1 = createTaskVariable();
        TaskVariableEntity taskVariable2 = createTaskVariable();
        taskVariableRepository.save(taskVariable1);
        taskVariableRepository.save(taskVariable2);

        Set<TaskVariableEntity> taskVariables = new HashSet<>();
        taskVariables.add(taskVariable1);
        taskVariables.add(taskVariable2);

        createTaskWithVariables(processInstanceEntity, taskVariables, processVariables);

        processInstanceRepository.save(processInstanceEntity);

        ProcessVariableInstance variableToFetch = processVariables.stream().findFirst().get();

        ValidatableMockMvcResponse response = given()
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
            .then();

        response.statusCode(200);
        response.body("_embedded.tasks", hasSize(1));
    }

    @NotNull
    private Set<ProcessVariableInstance> createProcessVariables(ProcessInstanceEntity processInstanceEntity) {
        Set<ProcessVariableInstance> variables = new HashSet<>();

        for (int i = 0; i < 8; i++) {
            ProcessVariableInstance ProcessVariableInstance = new ProcessVariableInstance();
            ProcessVariableInstance.setName("name" + i);
            ProcessVariableInstance.setValue("id");
            ProcessVariableInstance.setProcessInstanceId(processInstanceEntity.getId());
            ProcessVariableInstance.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
            variables.add(ProcessVariableInstance);
        }

        //TODO fix test
        //variableRepository.saveAll(variables);
        processInstanceEntity.setVariables(variables);
        processInstanceRepository.save(processInstanceEntity);
        return variables;
    }

    @NotNull
    private TaskVariableEntity createTaskVariable() {
        TaskVariableEntity taskVariableEntity = new TaskVariableEntity();
        taskVariableEntity.setName("name" + UUID.randomUUID());
        taskVariableEntity.setValue("var-value");
        return taskVariableEntity;
    }

    @NotNull
    private TaskEntity createTaskWithVariables(
        ProcessInstanceEntity processInstanceEntity,
        Set<TaskVariableEntity> taskVariables,
        Set<ProcessVariableInstance> processVariables
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
            taskVariableRepository.save(taskVariableEntity);
        });
        return taskEntity;
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setId("processInstanceId");
        processInstanceEntity.setName("name");
        processInstanceEntity.setInitiator("initiator");
        processInstanceEntity.setProcessDefinitionName("test");
        processInstanceEntity.setProcessDefinitionKey("processDefinitionKey");
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }
}
