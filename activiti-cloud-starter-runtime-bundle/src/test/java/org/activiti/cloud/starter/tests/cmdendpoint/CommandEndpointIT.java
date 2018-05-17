
/*
 * Copyright 2017 Alfresco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.starter.tests.cmdendpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakSecurityContextClientRequestInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.cloud.services.api.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.cloud.services.api.model.Task.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@ActiveProfiles(CommandEndPointITStreamHandler.COMMAND_ENDPOINT_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CommandEndpointIT {

    @Autowired
    private MessageClientStream clientStream;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CommandEndPointITStreamHandler streamHandler;

    private static final ParameterizedTypeReference<PagedResources<Task>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<Task>>() {
    };

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";
    private static final String PROCESS_INSTANCES_RELATIVE_URL = "/v1/process-instances/";
    private static final String TASKS_URL = "/v1/tasks/";

    private static final String SIMPLE_PROCESS = "SimpleProcess";

    @Autowired
    private KeycloakSecurityContextClientRequestInterceptor keycloakSecurityContextClientRequestInterceptor;

    @Before
    public void setUp() throws Exception {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        // Get Available Process Definitions
        ResponseEntity<PagedResources<ProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                                     pd.getId());
        }
    }

    @Test
    public void eventBasedStartProcessTests() throws Exception {

        Map<String, Object> vars = new HashMap<>();
        vars.put("hey",
                 "one");

        String simpleProcessDefinitionId = processDefinitionIds.get(SIMPLE_PROCESS);
        StartProcessInstanceCmd startProcessInstanceCmd = new StartProcessInstanceCmd(simpleProcessDefinitionId,
                                                                                      vars);

        String processInstanceId = startProcessInstance(startProcessInstanceCmd);

        SuspendProcessInstanceCmd suspendProcessInstanceCmd = new SuspendProcessInstanceCmd(processInstanceId);
        suspendProcessInstance(suspendProcessInstanceCmd);

        activateProcessInstance(simpleProcessDefinitionId,
                                processInstanceId);


        // Get Tasks

        //when
        ResponseEntity<PagedResources<Task>> responseEntity = getTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<Task> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks).extracting(Task::getStatus).contains(CREATED);

        Task task = tasks.iterator().next();

        // Claim Task
        claimTask(task);

        // Release Task
        releaseTask(task);

        // Reclaim Task to be able to complete it
        claimTask(task);

        // Complete Task
        completeTask(task);

        responseEntity = getTasks();
        tasks = responseEntity.getBody().getContent();
        assertThat(tasks)
                .filteredOn(t -> t.getId().equals(task.getId()))
                .isEmpty();

        // Checking that the process is finished
        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                                                                                                     HttpMethod.GET,
                                                                                                     null,
                                                                                                     new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                                                                     },
                                                                                                     "0",
                                                                                                     "2");

        assertThat(processInstancesPage.getBody().getContent())
                .filteredOn(processInstance -> processInstance.getId().equals(processInstanceId))
                .isEmpty();

        assertThat(streamHandler.getStartedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getSuspendedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getActivatedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getClaimedTaskAck()).isTrue();
        assertThat(streamHandler.getReleasedTaskAck()).isTrue();
        assertThat(streamHandler.getCompletedTaskAck()).isTrue();
    }

    private void completeTask(Task task) throws InterruptedException {
        Map<String, Object> variables = new HashMap<>();

        CompleteTaskCmd completeTaskCmd = new CompleteTaskCmd(task.getId(),
                                                              variables);
        String cmdId = UUID.randomUUID().toString();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(completeTaskCmd).setHeader("cmdId",
                                                                                 cmdId).build());
        await("task to be completed").untilTrue(streamHandler.getCompletedTaskAck());
    }

    private void releaseTask(Task task) throws InterruptedException {
        ReleaseTaskCmd releaseTaskCmd = new ReleaseTaskCmd(task.getId());
        String cmdId = UUID.randomUUID().toString();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(releaseTaskCmd).setHeader("cmdId",
                                                                                cmdId).build());
        await("task to be released").untilTrue(streamHandler.getReleasedTaskAck());

        assertThatTaskHasStatus(task.getId(),
                                CREATED);
    }

    private void claimTask(Task task) throws InterruptedException {
        ClaimTaskCmd claimTaskCmd = new ClaimTaskCmd(task.getId(),
                                                     "hruser");
        String cmdId = UUID.randomUUID().toString();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(claimTaskCmd).setHeader("cmdId",
                                                                              cmdId).build());

        await("task to be claimed").untilTrue(streamHandler.getClaimedTaskAck());

        assertThatTaskHasStatus(task.getId(),
                                ASSIGNED
        );
    }

    private void assertThatTaskHasStatus(String taskId,
                                         Task.TaskStatus status) {
        ResponseEntity<Task> responseEntity = getTask(taskId);
        Task retrievedTask = responseEntity.getBody();
        assertThat(retrievedTask.getStatus()).isEqualTo(status);
    }

    private void activateProcessInstance(String processDefinitionId,
                                         String processInstanceId) throws InterruptedException {
        //given
        ActivateProcessInstanceCmd activateProcessInstanceCmd = new ActivateProcessInstanceCmd(processInstanceId);
        String cmdId = UUID.randomUUID().toString();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(activateProcessInstanceCmd).setHeader("cmdId",
                                                                                            cmdId).build());

        await("process to be activated").untilTrue(streamHandler.getActivatedProcessInstanceAck());
        //when
        ProcessInstance processInstance = executeGetProcessInstanceRequest(processInstanceId);

        //then

        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinitionId);
        assertThat(processInstance.getId()).isNotNull();
        assertThat(processInstance.getStartDate()).isNotNull();
        assertThat(processInstance.getStatus()).isEqualToIgnoringCase(ProcessInstance.ProcessInstanceStatus.RUNNING.name());
    }

    private void suspendProcessInstance(SuspendProcessInstanceCmd suspendProcessInstanceCmd) throws InterruptedException {
        //given

        clientStream.myCmdProducer().send(MessageBuilder.withPayload(suspendProcessInstanceCmd).build());

        await("process to be suspended").untilTrue(streamHandler.getSuspendedProcessInstanceAck());
        //when
        ProcessInstance processInstance = executeGetProcessInstanceRequest(suspendProcessInstanceCmd.getProcessInstanceId());

        //then
        assertThat(processInstance.getId()).isEqualTo(suspendProcessInstanceCmd.getProcessInstanceId());
        assertThat(processInstance.getStartDate()).isNotNull();
        assertThat(processInstance.getStatus()).isEqualToIgnoringCase(ProcessInstance.ProcessInstanceStatus.SUSPENDED.name());
    }

    private String startProcessInstance(StartProcessInstanceCmd startProcessInstanceCmd) throws InterruptedException {
        //given
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(startProcessInstanceCmd).build());

        await("process to be started")
                .untilTrue(streamHandler.getStartedProcessInstanceAck());
        String processInstanceId = streamHandler.getProcessInstanceId();

        //when
        ProcessInstance processInstance = executeGetProcessInstanceRequest(processInstanceId);

        //then
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(startProcessInstanceCmd.getProcessDefinitionId());
        assertThat(processInstance.getId()).isNotNull();
        assertThat(processInstance.getStartDate()).isNotNull();
        assertThat(processInstance.getStatus()).isEqualToIgnoringCase(ProcessInstance.ProcessInstanceStatus.RUNNING.name());
        return processInstance.getId();
    }

    private ProcessInstance executeGetProcessInstanceRequest(String processInstanceId) {
        ResponseEntity<ProcessInstance> processInstanceResponseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "{processInstanceId}",
                                                                                              HttpMethod.GET,
                                                                                              null,
                                                                                              new ParameterizedTypeReference<ProcessInstance>() {
                                                                                                     },
                                                                                              processInstanceId);

        assertThat(processInstanceResponseEntity).isNotNull();
        assertThat(processInstanceResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        ProcessInstance processInstance = processInstanceResponseEntity.getBody();
        assertThat(processInstance).isNotNull();
        return processInstance;
    }

    private ResponseEntity<PagedResources<Task>> getTasks() {
        return restTemplate.exchange(TASKS_URL,
                                     HttpMethod.GET,
                                     null,
                                     PAGED_TASKS_RESPONSE_TYPE);
    }

    private ResponseEntity<Task> getTask(String taskId) {
        ResponseEntity<Task> responseEntity = restTemplate.exchange(TASKS_URL + taskId,
                                                              HttpMethod.GET,
                                                              null,
                                                              new ParameterizedTypeReference<Task>() {
                                                              });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    private ResponseEntity<PagedResources<ProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<ProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<ProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }
}
