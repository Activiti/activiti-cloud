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
package org.activiti.cloud.starter.tests.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.impl.CandidateGroup;
import org.activiti.cloud.api.process.model.impl.CandidateUser;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessDefinitionRestTemplate;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.cloud.starter.tests.util.VariablesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties", "classpath:access-control.properties"})
@DirtiesContext
@ContextConfiguration(classes = RuntimeITConfiguration.class,initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class TasksIT {

    private static final String SIMPLE_PROCESS = "SimpleProcess";

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private ProcessDefinitionRestTemplate processDefinitionRestTemplate;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    @Autowired
    private  VariablesUtil variablesUtil;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @BeforeEach
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = processDefinitionRestTemplate.getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                                     pd.getId());
        }
    }

    @Test
    public void shouldGetAvailableTasks() {
        //we are hruser who is in hr group so we can see tasks

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<PagedModel<CloudTask>> responseEntity = taskRestTemplate.getTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void taskShouldHaveFormKey() {
        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<PagedModel<CloudTask>> responseEntity = taskRestTemplate.getTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getFormKey).contains("taskFormKey");
    }

    @Test
    public void shouldUpdateNameDescription() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        ResponseEntity<PagedModel<CloudTask>> responseEntity = processInstanceRestTemplate.getTasks(processInstanceEntity);
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        CloudTask task = tasks.iterator().next();
        taskRestTemplate.claim(task);

        UpdateTaskPayload updateTask = TaskPayloadBuilder.update().withTaskId(task.getId())
                .withName("Updated name")
                .withDescription("Updated description")
                .build();

        //when
        taskRestTemplate.updateTask(updateTask);

        //then
        ResponseEntity<CloudTask> taskResponseEntity = taskRestTemplate.getTask(task.getId());

        assertThat(taskResponseEntity.getBody().getName()).isEqualTo("Updated name");
        assertThat(taskResponseEntity.getBody().getDescription()).isEqualTo("Updated description");

        //Check UpdateTaskPayload without taskId
        updateTask = TaskPayloadBuilder.update()
                .withName("New Updated name")
                .withDescription("New Updated description")
                .build();

        //when
        taskRestTemplate.updateTask(task.getId(),updateTask);

        //then
        taskResponseEntity = taskRestTemplate.getTask(task.getId());

        assertThat(taskResponseEntity.getBody().getName()).isEqualTo("New Updated name");
        assertThat(taskResponseEntity.getBody().getDescription()).isEqualTo("New Updated description");
    }

    @Test
    public void adminShouldUpdateNameDescription() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        ResponseEntity<PagedModel<CloudTask>> responseEntity = processInstanceRestTemplate.getTasks(processInstanceEntity);
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        CloudTask task = tasks.iterator().next();

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        UpdateTaskPayload updateTask = TaskPayloadBuilder.update().withTaskId(task.getId())
                .withName("Updated name")
                .withDescription("Updated description")
                .build();

        //when
        taskRestTemplate.adminUpdateTask(updateTask);

        //then
        //once admin/v1/tasks/{taskId} is available there will be no need to switch users
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");
        ResponseEntity<CloudTask> taskResponseEntity = taskRestTemplate.getTask(task.getId());

        assertThat(taskResponseEntity.getBody().getName()).isEqualTo("Updated name");
        assertThat(taskResponseEntity.getBody().getDescription()).isEqualTo("Updated description");

        //Check UpdateTaskPayload without taskId
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        updateTask = TaskPayloadBuilder.update()
                .withName("New Updated name")
                .withDescription("New Updated description")
                .build();

        //when
        taskRestTemplate.adminUpdateTask(task.getId(),updateTask);

        //then
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");
        taskResponseEntity = taskRestTemplate.getTask(task.getId());

        assertThat(taskResponseEntity.getBody().getName()).isEqualTo("New Updated name");
        assertThat(taskResponseEntity.getBody().getDescription()).isEqualTo("New Updated description");
    }

    @Test
    public void shouldNotGetTasksWithoutPermission() {

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //SIMPLE_PROCESS not visible to testuser according to access-control.properties
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");

        //when
        ResponseEntity<PagedModel<CloudTask>> responseEntity = taskRestTemplate.getTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks.size()).isEqualTo(0);
    }

    @Test
    public void shouldNotSeeAdminTasks() {

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<PagedModel<CloudTask>> responseEntity = taskRestTemplate.adminGetTasks();

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminShouldGetAvailableTasksAtAdminEndpoint() {

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");

        //when
        ResponseEntity<PagedModel<CloudTask>> responseEntity = taskRestTemplate.adminGetTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    public void shouldGetTasksRelatedToTheGivenProcessInstance() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<PagedModel<CloudTask>> tasksEntity = processInstanceRestTemplate.getTasks(startProcessResponse);

        //then
        assertThat(tasksEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tasksEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Perform action");
    }

    @Test
    public void shouldGetSubTasks() {
        //given
        CloudTask parentTask = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("parent task").withDescription("This is my parent task").build());

        CreateTaskPayload createSubTask = TaskPayloadBuilder.create().withName("sub task").withDescription("This is my sub-task").withParentTaskId(parentTask.getId()).build();

        CloudTask subTask = taskRestTemplate.createTask(createSubTask);

        //when
        PagedModel<CloudTask> subTasks = taskRestTemplate.getSubTasks(parentTask);

        //then
        assertThat(subTasks.getContent()).extracting(CloudTask::getId).containsExactly(subTask.getId());
    }

    @Test
    public void shouldBeAbleToDeleteTask() {
        //given
        CloudTask standaloneTask = taskRestTemplate.createTask(TaskPayloadBuilder.create()
            .withName("parent task")
            .withDescription("This is my parent task")
            .withAssignee("hruser")
            .build());
        //when
        ResponseEntity<CloudTask> delete = taskRestTemplate.delete(standaloneTask);

        //then
        assertThat(delete.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void adminShouldBeAbleToDeleteTask() {
        //given
        CloudTask standaloneTask = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("parent task").withDescription("This is my parent task").build());
        //when
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        ResponseEntity<CloudTask> delete = taskRestTemplate.adminDelete(standaloneTask);

        //then
        assertThat(delete.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void shouldGetTaskById() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        //when
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.getTask(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody().getId()).isEqualTo(task.getId());
    }

    @Test
    public void adminShouldGetTaskById() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        //when
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.adminGetTask(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isEqualToComparingFieldByField(task);
    }

    @Test
    public void claimTaskShouldSetAssignee() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        //when
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.claim(task);

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getAssignee()).isEqualTo(keycloakSecurityContextClientRequestInterceptor.getKeycloakTestUser());
    }

    @Test
    public void releaseTaskShouldSetAssigneeBackToNull() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        taskRestTemplate.claim(task);

        //when
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.release(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody().getAssignee()).isNull();
    }

    @Test
    public void shouldCompleteATask() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();
        taskRestTemplate.claim(task);

        //when
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.complete(task);

        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void adminShouldCompleteATask() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        //when
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.adminComplete(task);

        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void shouldCompleteATaskPassingInputVariables() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();
        taskRestTemplate.claim(task);

        CompleteTaskPayload completeTaskPayload = TaskPayloadBuilder.complete().withTaskId(task.getId()).withVariables(Collections.singletonMap("myVar",
                                                                                                                                                "any")).build();

        //when
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.complete(task,completeTaskPayload);


        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    public void adminShouldAssignUser() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        String taskId = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next().getId();

        //when
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.adminGetTask(taskId);
        assertThat(responseEntity).isNotNull();

        //then
        Task task = responseEntity.getBody();
        assertThat(task.getAssignee()).isNull();

        //when
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        AssignTaskPayload assignTaskPayload = TaskPayloadBuilder
                                              .assign()
                                              .withTaskId(task.getId())
                                              .withAssignee("hruser")
                                              .build();

        ResponseEntity<CloudTask> assignResponseEntity = taskRestTemplate.adminAssignTask(assignTaskPayload);

        //then
        assertThat(assignResponseEntity).isNotNull();
        assertThat(assignResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assignResponseEntity.getBody().getAssignee()).isEqualTo("hruser");
    }

    @Test
    public void shouldAddUserCandidateAndClaimTaskAnotherUser() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        //then check that we have one candidate
        ResponseEntity<CollectionModel<EntityModel<CandidateUser>>> userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        assertThat(userCandidates).isNotNull();
        assertThat(userCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().getContent().size()).isEqualTo(1);
        assertThat(userCandidates.getBody().getContent()
                           .stream()
                           .map(EntityModel::getContent)
                           .map(CandidateUser::getUser)
        ).containsExactly("hruser");
        taskRestTemplate.claim(task);

        //when
        CandidateUsersPayload candidateusers = TaskPayloadBuilder
                .addCandidateUsers()
                .withTaskId(task.getId())
                .withCandidateUser("testuser")
                .build();
        ResponseEntity<Void> responseEntity = taskRestTemplate.addUserCandidates(candidateusers);

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().getContent().size()).isEqualTo(2);
        assertThat(userCandidates.getBody().getContent()
                           .stream()
                           .map(EntityModel::getContent)
                           .map(CandidateUser::getUser)
        ).containsExactly("hruser",
                          "testuser");

        //when
        taskRestTemplate.release(task);

        //Claim task by another user
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");
        ResponseEntity<CloudTask> responseTask = taskRestTemplate.claim(task);

        //then
        assertThat(responseTask).isNotNull();
        assertThat(responseTask.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseTask.getBody().getAssignee()).isEqualTo("testuser");

    }

    @Test
    public void shouldAddDeleteUserCandidate() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        //then check that we have one candidate
        ResponseEntity<CollectionModel<EntityModel<CandidateUser>>> userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        assertThat(userCandidates).isNotNull();
        assertThat(userCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().getContent().size()).isEqualTo(1);
        assertThat(userCandidates.getBody().getContent()
                           .stream()
                           .map(EntityModel::getContent)
                           .map(CandidateUser::getUser)
        ).containsExactly("hruser");

        taskRestTemplate.claim(task);

        //when
        CandidateUsersPayload candidateusers = TaskPayloadBuilder
                .addCandidateUsers()
                .withTaskId(task.getId())
                .withCandidateUser("testuser")
                .build();
        ResponseEntity<Void> responseEntity = taskRestTemplate.addUserCandidates(candidateusers);

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        userCandidates = taskRestTemplate.getUserCandidates(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().getContent().size()).isEqualTo(2);
        assertThat(userCandidates.getBody().getContent()
                           .stream()
                           .map(EntityModel::getContent)
                           .map(CandidateUser::getUser)
        ).containsExactly("hruser",
                          "testuser");


        candidateusers = TaskPayloadBuilder
                .addCandidateUsers()
                .withTaskId(task.getId())
                .withCandidateUser("testuser")
                .build();
        responseEntity = taskRestTemplate.deleteUserCandidates(candidateusers);

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().getContent().size()).isEqualTo(1);
        assertThat(userCandidates.getBody().getContent()
                           .stream()
                           .map(EntityModel::getContent)
                           .map(CandidateUser::getUser)
        ).containsExactly("hruser");

    }

    @Test
    public void shouldDeleteAddGroupCandidate() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();

        //then check that we have no group candidate
        ResponseEntity<CollectionModel<EntityModel<CandidateGroup>>> groupCandidates = taskRestTemplate.getGroupCandidates(task.getId());
        assertThat(groupCandidates).isNotNull();
        assertThat(groupCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(groupCandidates.getBody().getContent().size()).isEqualTo(1);
        assertThat(groupCandidates.getBody().getContent()
                           .stream()
                           .map(EntityModel::getContent)
                           .map(CandidateGroup::getGroup)
        ).containsExactly("hr");


        taskRestTemplate.claim(task);

        //when
        CandidateGroupsPayload candidategroups = TaskPayloadBuilder
                .deleteCandidateGroups()
                .withTaskId(task.getId())
                .withCandidateGroup("hr")
                .build();
        ResponseEntity<Void> responseEntity = taskRestTemplate.deleteGroupCandidates(candidategroups);

        //then
        groupCandidates = taskRestTemplate.getGroupCandidates(task.getId());
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(groupCandidates.getBody().getContent().size()).isEqualTo(0);

        //when
        candidategroups = TaskPayloadBuilder
                .addCandidateGroups()
                .withTaskId(task.getId())
                .withCandidateGroup("hr")
                .build();

        responseEntity = taskRestTemplate.addGroupCandidates(candidategroups);

        //then
        groupCandidates = taskRestTemplate.getGroupCandidates(task.getId());
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(groupCandidates.getBody().getContent().size()).isEqualTo(1);
        assertThat(groupCandidates.getBody().getContent()
                           .stream()
                           .map(EntityModel::getContent)
                           .map(CandidateGroup::getGroup)
        ).containsExactly("hr");

    }

    @Test
    public void shouldSaveATask() throws Exception {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();
        taskRestTemplate.claim(task);

        Map<String, Object> variables = new HashMap<>();
        Date date = new Date();

        variables.put("variableInt",
                      2);
        variables.put("variableStr",
                      "new value");
        variables.put("variableBool",
                      false);
        variables.put("variableDateTime",
                      variablesUtil.getDateTimeFormattedString(date));
        variables.put("variableDate",
                      variablesUtil.getDateFormattedString(date));

        SaveTaskPayload saveTaskPayload = TaskPayloadBuilder.save()
                                                            .withTaskId(task.getId())
                                                            .withVariables(variables)
                                                            .build();
        //when
        ResponseEntity<Void> responseEntity = taskRestTemplate.save(task, saveTaskPayload);

        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());

        // when
        ResponseEntity<CollectionModel<CloudVariableInstance>> variablesResponse = taskRestTemplate.getVariables(task.getId());

        // then
        assertThat(variablesResponse).isNotNull();
        assertThat(variablesResponse.getBody().getContent()).extracting(CloudVariableInstance::getName,
                                                                        CloudVariableInstance::getType,
                                                                        CloudVariableInstance::getValue)
                                                            .containsExactlyInAnyOrder(
                                                                             tuple("variableInt",
                                                                                   "integer",
                                                                                   2),
                                                                             tuple("variableStr",
                                                                                   "string",
                                                                                   "new value"),
                                                                             tuple("variableBool",
                                                                                   "boolean",
                                                                                   false),
                                                                             tuple("variableDateTime",
                                                                                   "date",
                                                                                   variablesUtil.getExpectedDateTimeFormattedString(date)),
                                                                             tuple("variableDate",
                                                                                   "date",
                                                                                   variablesUtil.getExpectedDateFormattedString(date))
                                                                             );
        // cleanup
        processInstanceRestTemplate.delete(processInstanceEntity);

    }

    @Test
    public void shouldNotSaveATaskWithEmptyPayload() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();
        taskRestTemplate.claim(task);

        SaveTaskPayload saveTaskPayload = null;

        //when
        ResponseEntity<Void> responseEntity = taskRestTemplate.save(task, saveTaskPayload);

        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        // cleanup
        processInstanceRestTemplate.delete(processInstanceEntity);
    }


}
