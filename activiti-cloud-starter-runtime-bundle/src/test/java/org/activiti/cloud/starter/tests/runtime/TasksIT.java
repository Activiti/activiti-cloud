/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.starter.tests.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CandidateGroupsPayload;
import org.activiti.api.task.model.payloads.CandidateUsersPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource({"classpath:application-test.properties", "classpath:access-control.properties"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TasksIT {

    private static final String TASKS_URL = "/v1/tasks/";
    private static final String ADMIN_TASKS_URL = "/admin/v1/tasks/";
    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final ParameterizedTypeReference<CloudTask> TASK_RESPONSE_TYPE = new ParameterizedTypeReference<CloudTask>() {
    };
    private static final ParameterizedTypeReference<PagedResources<CloudTask>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<CloudTask>>() {
    };
    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private KeycloakTokenProducer keycloakSecurityContextClientRequestInterceptor;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Before
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
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
        ResponseEntity<PagedResources<CloudTask>> responseEntity = executeRequestGetTasks();

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
        ResponseEntity<PagedResources<CloudTask>> responseEntity = executeRequestGetTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getFormKey).contains("taskFormKey");
    }

    @Test
    public void shouldUpdateDescription() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        ResponseEntity<PagedResources<CloudTask>> responseEntity = processInstanceRestTemplate.getTasks(processInstanceEntity);
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        CloudTask task = tasks.iterator().next();
        taskRestTemplate.claim(task);

        UpdateTaskPayload updateTask = TaskPayloadBuilder.update().withTaskId(task.getId()).withDescription("Updated description").build();

        //when
        taskRestTemplate.updateTask(updateTask);

        //then
        ResponseEntity<CloudTask> taskResponseEntity = taskRestTemplate.getTask(task.getId());

        assertThat(taskResponseEntity.getBody().getDescription()).isEqualTo("Updated description");
    }

    @Test
    public void shouldNotGetTasksWithoutPermission() {

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //SIMPLE_PROCESS not visible to testuser according to access-control.properties
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");

        //when
        ResponseEntity<PagedResources<CloudTask>> responseEntity = executeRequestGetTasks();

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
        ResponseEntity<PagedResources<CloudTask>> responseEntity = executeRequestGetAdminTasks();

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
        ResponseEntity<PagedResources<CloudTask>> responseEntity = executeRequestGetAdminTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks.size()).isGreaterThanOrEqualTo(2);
    }
    
    private ResponseEntity<PagedResources<CloudTask>> executeRequestGetTasks() {
        return testRestTemplate.exchange(TASKS_URL,
                                         HttpMethod.GET,
                                         null,
                                         PAGED_TASKS_RESPONSE_TYPE);
    }

    private ResponseEntity<PagedResources<CloudTask>> executeRequestGetAdminTasks() {
        return testRestTemplate.exchange(ADMIN_TASKS_URL,
                                         HttpMethod.GET,
                                         null,
                                         PAGED_TASKS_RESPONSE_TYPE);
    }
    
    @Test
    public void shouldGetTasksRelatedToTheGivenProcessInstance() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessResponse = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

        //when
        ResponseEntity<PagedResources<CloudTask>> tasksEntity = testRestTemplate.exchange(ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL + startProcessResponse.getBody().getId() + "/tasks",
                                                                                          HttpMethod.GET,
                                                                                          null,
                                                                                          PAGED_TASKS_RESPONSE_TYPE);

        //then
        assertThat(tasksEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tasksEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Perform action");
    }

    @Test
    public void shouldGetSubTasks() {
        //given
        CloudTask parentTask = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("parent task").withDescription("This is my parent task").build());

        CreateTaskPayload createSubTask = TaskPayloadBuilder.create().withName("sub task").withDescription("This is my sub-task").withParentTaskId(parentTask.getId()).build();

        CloudTask subTask = taskRestTemplate.createSubTask(createSubTask);

        //when
        PagedResources<CloudTask> subTasks = taskRestTemplate.getSubTasks(parentTask);

        //then
        assertThat(subTasks.getContent()).extracting(CloudTask::getId).containsExactly(subTask.getId());
    }

    @Test
    public void shouldBeAbleToDeleteTask() {
        //given
        CloudTask standaloneTask = taskRestTemplate.createTask(TaskPayloadBuilder.create().withName("parent task").withDescription("This is my parent task").build());
        //when
        ResponseEntity<CloudTask> delete = taskRestTemplate.delete(standaloneTask);

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
        ResponseEntity<CloudTask> responseEntity = testRestTemplate.exchange(TASKS_URL + task.getId() + "/release",
                                                                             HttpMethod.POST,
                                                                             null,
                                                                             TASK_RESPONSE_TYPE);

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
        ResponseEntity<Task> responseEntity = taskRestTemplate.complete(task);

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
        ResponseEntity<Task> responseEntity = testRestTemplate.exchange(TASKS_URL + task.getId() + "/complete",
                                                                        HttpMethod.POST,
                                                                        new HttpEntity<>(completeTaskPayload),
                                                                        new ParameterizedTypeReference<Task>() {
                                                                        });

        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }
    
    @Test
    public void adminShouldAssignUser() {
        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
  
        //when
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testadmin");
        ResponseEntity<PagedResources<CloudTask>> responseEntity = executeRequestGetAdminTasks();
        assertThat(responseEntity).isNotNull();
            
        //then
        Task task = responseEntity.getBody().iterator().next();
        assertThat(task.getAssignee()).isNull();
        
        //when
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
        
        //restore user
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");
    }
    
    @Test
    public void shouldAddUserCandidateAndClaimTaskAnotherUser() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();
        
        //then check that we have one candidate
        ResponseEntity<List<String>> userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        assertThat(userCandidates).isNotNull();
        assertThat(userCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().size()).isEqualTo(1);
        assertThat(userCandidates.getBody().get(0)).isEqualTo("hruser");
          
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
        assertThat(userCandidates.getBody().size()).isEqualTo(2);
        assertThat(userCandidates.getBody().get(0)).isEqualTo("hruser");
        assertThat(userCandidates.getBody().get(1)).isEqualTo("testuser");
        
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
        ResponseEntity<List<String>> userCandidates = taskRestTemplate.getUserCandidates(task.getId());
        assertThat(userCandidates).isNotNull();
        assertThat(userCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userCandidates.getBody().size()).isEqualTo(1);
        assertThat(userCandidates.getBody().get(0)).isEqualTo("hruser");
          
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
        assertThat(userCandidates.getBody().size()).isEqualTo(2);
        assertThat(userCandidates.getBody().get(0)).isEqualTo("hruser");
        assertThat(userCandidates.getBody().get(1)).isEqualTo("testuser");
        
        
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
        assertThat(userCandidates.getBody().size()).isEqualTo(1);
        assertThat(userCandidates.getBody().get(0)).isEqualTo("hruser");
        
    }
    
    @Test
    public void shouldDeleteAddGroupCandidate() {
        //given
        ResponseEntity<CloudProcessInstance> processInstanceEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = processInstanceRestTemplate.getTasks(processInstanceEntity).getBody().iterator().next();
        
        //then check that we have no group candidate
        ResponseEntity<List<String>> groupCandidates = taskRestTemplate.getGroupCandidates(task.getId());
        assertThat(groupCandidates).isNotNull();
        assertThat(groupCandidates.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(groupCandidates.getBody().size()).isEqualTo(1);
        assertThat(groupCandidates.getBody().get(0)).isEqualTo("hr");
  
          
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
        assertThat(groupCandidates.getBody().size()).isEqualTo(0);
        
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
        
        assertThat(groupCandidates.getBody().size()).isEqualTo(1);
        assertThat(groupCandidates.getBody().get(0)).isEqualTo("hr");

    }
 
    private ResponseEntity<PagedResources<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
        };

        return testRestTemplate.exchange(PROCESS_DEFINITIONS_URL,
                                         HttpMethod.GET,
                                         null,
                                         responseType);
    }
}