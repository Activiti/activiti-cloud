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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakSecurityContextClientRequestInterceptor;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.runtime.api.cmd.impl.CompleteTaskImpl;
import org.activiti.runtime.api.cmd.impl.UpdateTaskImpl;
import org.activiti.runtime.api.model.CloudProcessDefinition;
import org.activiti.runtime.api.model.CloudProcessInstance;
import org.activiti.runtime.api.model.CloudTask;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.Task;
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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TasksIT  {

    private static final String TASKS_URL = "/v1/tasks/";
    private static final String ADMIN_TASKS_URL = "/admin/v1/tasks/";
    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final ParameterizedTypeReference<CloudTask> TASK_RESPONSE_TYPE = new ParameterizedTypeReference<CloudTask>() {};
    private static final ParameterizedTypeReference<PagedResources<CloudTask>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<CloudTask>>() {
    };
    public static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";


    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private KeycloakSecurityContextClientRequestInterceptor keycloakSecurityContextClientRequestInterceptor;

    private Map<String, String> processDefinitionIds = new HashMap<>();


    @Before
    public void setUp(){
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");


        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for(ProcessDefinition pd : processDefinitions.getBody().getContent()){
            processDefinitionIds.put(pd.getName(), pd.getId());
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
    public void shouldUpdateDescription() {
        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        ResponseEntity<PagedResources<CloudTask>> responseEntity = executeRequestGetTasks();
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        CloudTask task = tasks.iterator().next();

        UpdateTaskImpl updateTask = new UpdateTaskImpl();
        updateTask.setDescription("Updated description");

        //when
        taskRestTemplate.updateTask(task, updateTask);

        //then
        ResponseEntity<CloudTask> taskResponseEntity = taskRestTemplate.getTask(task.getId());

        assertThat(taskResponseEntity.getBody().getDescription()).isEqualTo("Updated description");

    }

    @Test
    public void shouldNotGetTasksWithoutPermission() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("testuser");

        //now authenticated as testuser who is not in hr group

        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));

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
    public void shouldGetTaskById() {
        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = executeRequestGetTasks().getBody().iterator().next();

        //when
        ResponseEntity<CloudTask> responseEntity = taskRestTemplate.getTask(task.getId());

        //then
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isEqualToComparingFieldByField(task);
    }


    @Test
    public void claimTaskShouldSetAssignee() {
        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = executeRequestGetTasks().getBody().iterator().next();

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
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = executeRequestGetTasks().getBody().iterator().next();

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
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = executeRequestGetTasks().getBody().iterator().next();

        //when
        ResponseEntity<Void> responseEntity = taskRestTemplate.complete(task);

        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }


    @Test
    public void shouldCompleteATaskPassingInputVariables() {
        //given
        processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIMPLE_PROCESS));
        Task task = executeRequestGetTasks().getBody().iterator().next();

        CompleteTaskImpl completeTaskCmd = new CompleteTaskImpl(task.getId(), Collections.singletonMap("myVar",
                                                                                                      "any"));

        //when
        ResponseEntity<Void> responseEntity = testRestTemplate.exchange(TASKS_URL + task.getId() + "/complete",
                HttpMethod.POST,
                new HttpEntity<>(completeTaskCmd),
                new ParameterizedTypeReference<Void>() {
                });

        //then
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
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