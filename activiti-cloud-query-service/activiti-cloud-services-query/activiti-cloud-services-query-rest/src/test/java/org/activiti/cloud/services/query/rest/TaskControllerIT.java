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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.List;
import java.util.UUID;
import org.activiti.QueryRestTestApplication;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.alfresco.config.AlfrescoWebAutoConfiguration;
import org.activiti.cloud.services.query.app.repository.ProcessInstanceRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
    properties = {
        "spring.main.banner-mode=off",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    }
)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
class TaskControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private TaskCandidateGroupRepository taskCandidateGroupRepository;

    @Autowired
    private TaskCandidateUserRepository taskCandidateUserRepository;

    @SpyBean
    private SecurityManager securityManager;

    private static final String CURRENT_USER = "testuser";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeEach
    public void setUp() {
        webAppContextSetup(context);
        postProcessors(csrf().asHeader());
        taskRepository.deleteAll();
        processInstanceRepository.deleteAll();
        taskCandidateGroupRepository.deleteAll();
        taskCandidateUserRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = CURRENT_USER)
    void should_returnTasks_withOnlyRequestedProcessVariables_whenSearchingByTaskVariableNameAndValue() {
        ProcessInstanceEntity processInstance = createProcessInstance();
        String otherUser = "other-user";
        processInstance.setInitiator(otherUser);
        processInstanceRepository.save(processInstance);

        //Task to be retrieved because user is assignee
        TaskEntity task1 = new TaskEntity();
        task1.setId(UUID.randomUUID().toString());
        task1.setProcessInstanceId(processInstance.getId());
        task1.setAssignee(CURRENT_USER);
        task1.setOwner(otherUser);
        taskRepository.save(task1);

        //Task to be retrieved because user is candidate for the task and task is not assigned
        TaskEntity task2 = new TaskEntity();
        task2.setId(UUID.randomUUID().toString());
        task2.setProcessInstanceId(processInstance.getId());
        taskRepository.save(task2);
        TaskCandidateUserEntity taskCandidateTask2 = new TaskCandidateUserEntity();
        taskCandidateTask2.setUserId(CURRENT_USER);
        taskCandidateTask2.setTaskId(task2.getId());
        taskCandidateUserRepository.save(taskCandidateTask2);

        //Task NOT to be retrieved because user is candidate for the task but task is assigned
        TaskEntity task3 = new TaskEntity();
        task3.setId(UUID.randomUUID().toString());
        task3.setProcessInstanceId(processInstance.getId());
        task3.setAssignee(otherUser);
        taskRepository.save(task3);
        TaskCandidateUserEntity taskCandidateTask3 = new TaskCandidateUserEntity();
        taskCandidateTask3.setUserId(CURRENT_USER);
        taskCandidateTask3.setTaskId(task2.getId());
        taskCandidateUserRepository.save(taskCandidateTask3);

        //Task to be retrieved because user belongs to candidate group and task is not assigned
        TaskEntity task4 = new TaskEntity();
        task4.setId(UUID.randomUUID().toString());
        task4.setProcessInstanceId(processInstance.getId());
        taskRepository.save(task4);
        TaskCandidateGroupEntity taskCandidateGroup = new TaskCandidateGroupEntity();
        taskCandidateGroup.setGroupId("testgroup");
        taskCandidateGroup.setTaskId(task3.getId());
        taskCandidateGroupRepository.save(taskCandidateGroup);

        //Task NOT to be retrieved because user belongs to candidate group but task is assigned
        TaskEntity task5 = new TaskEntity();
        task5.setId(UUID.randomUUID().toString());
        task5.setProcessInstanceId(processInstance.getId());
        task5.setAssignee(otherUser);
        taskRepository.save(task5);
        TaskCandidateGroupEntity taskCandidateGroup2 = new TaskCandidateGroupEntity();
        taskCandidateGroup2.setGroupId("testgroup");
        taskCandidateGroup2.setTaskId(task5.getId());
        taskCandidateGroupRepository.save(taskCandidateGroup2);

        //Task to be retrieved because user is owner
        TaskEntity task6 = new TaskEntity();
        task6.setId(UUID.randomUUID().toString());
        task6.setProcessInstanceId(processInstance.getId());
        task6.setAssignee(otherUser);
        task6.setOwner(CURRENT_USER);
        taskRepository.save(task6);

        //Task to be retrieved because there are no candidate users and groups and task is not assigned
        TaskEntity task7 = new TaskEntity();
        task7.setId(UUID.randomUUID().toString());
        task7.setProcessInstanceId(processInstance.getId());
        task7.setOwner(otherUser);
        taskRepository.save(task7);

        Mockito.when(securityManager.getAuthenticatedUserGroups()).thenReturn(List.of("testgroup"));

        given()
            .webAppContextSetup(context)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post("/v1/tasks/search")
            .then()
            .statusCode(200)
            .body("_embedded.tasks", hasSize(5))
            .body(
                "_embedded.tasks",
                Matchers.hasItems(
                    Matchers.hasEntry("id", task1.getId()),
                    Matchers.hasEntry("id", task2.getId()),
                    Matchers.hasEntry("id", task4.getId()),
                    Matchers.hasEntry("id", task6.getId()),
                    Matchers.hasEntry("id", task7.getId())
                )
            );
    }

    @NotNull
    private ProcessInstanceEntity createProcessInstance() {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        String process = UUID.randomUUID().toString();
        processInstanceEntity.setId(process);
        processInstanceEntity.setName(process);
        processInstanceEntity.setInitiator("testuser");
        processInstanceEntity.setProcessDefinitionName(process);
        processInstanceEntity.setProcessDefinitionKey(process);
        processInstanceEntity.setServiceName("test");
        processInstanceRepository.save(processInstanceEntity);
        return processInstanceEntity;
    }
}
