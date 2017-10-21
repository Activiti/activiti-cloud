
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

import org.activiti.cloud.services.core.model.ProcessDefinition;
import org.activiti.cloud.services.core.model.ProcessInstance;
import org.activiti.cloud.services.core.model.Task;
import org.activiti.cloud.services.core.model.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.core.model.commands.ClaimTaskCmd;
import org.activiti.cloud.services.core.model.commands.CompleteTaskCmd;
import org.activiti.cloud.services.core.model.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.core.model.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.core.model.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.identity.keycloak.interceptor.KeycloakSecurityContextClientRequestInterceptor;
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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

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

    public static final ParameterizedTypeReference<PagedResources<Task>> PAGED_TASKS_RESPONSE_TYPE = new ParameterizedTypeReference<PagedResources<Task>>() {
    };

    private Map<String, String> processDefinitionIds = new HashMap<>();

    public static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";
    public static final String PROCESS_INSTANCES_RELATIVE_URL = "/v1/process-instances/";
    private static final String TASKS_URL = "/v1/tasks/";

    private static final String SIMPLE_PROCESS = "SimpleProcess";

    private StartProcessInstanceCmd startProcessInstanceCmd;

    private SuspendProcessInstanceCmd suspendProcessInstanceCmd;

    @Autowired
    private KeycloakSecurityContextClientRequestInterceptor keycloakSecurityContextClientRequestInterceptor;

    @Before
    public void setUp() throws Exception {
        keycloakSecurityContextClientRequestInterceptor.setKeycloaktestuser("hruser");

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

        //record what instances there were before starting this one - should be none but will check this later
        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPageBefore = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                                                                                                           HttpMethod.GET,
                                                                                                           null,
                                                                                                           new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                                                                           },
                                                                                                           "0",
                                                                                                           "2");
        Map<String, Object> vars = new HashMap<>();
        vars.put("hey",
                 "one");

        startProcessInstanceCmd = new StartProcessInstanceCmd(processDefinitionIds.get(SIMPLE_PROCESS),
                                                              vars);

        // Start a Process Instance sending a message
        String processInstanceId = startProcessInstance(processDefinitionIds.get(SIMPLE_PROCESS),
                                                        processInstancesPageBefore);

        suspendProcessInstanceCmd = new SuspendProcessInstanceCmd(streamHandler.getProcessInstanceId());
        // Suspending a Process Instance sending a message
        suspendProcessInstance(processDefinitionIds.get(SIMPLE_PROCESS));

        // Activating a Process Instance sending a message
        activateProcessInstance(processDefinitionIds.get(SIMPLE_PROCESS),
                                processInstanceId);

        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                                                                                                     HttpMethod.GET,
                                                                                                     null,
                                                                                                     new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                                                                     },
                                                                                                     "0",
                                                                                                     "2");

        //should have only started one
        assertThat(processInstancesPage.getBody().getContent().size() - processInstancesPageBefore.getBody().getContent().size()).isEqualTo(1);

        //expecting we started with none
        assertThat(processInstancesPageBefore.getBody().getContent()).hasSize(0);

        assertThat(processInstancesPage.getBody().getContent()).hasSize(1);
        assertThat(processInstancesPage.getBody().getMetadata().getTotalPages()).isEqualTo(1);

        // Get Tasks

        //when
        ResponseEntity<PagedResources<Task>> responseEntity = getTasks();

        //then
        assertThat(responseEntity).isNotNull();
        Collection<Task> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks).extracting(Task::getStatus).contains(Task.TaskStatus.CREATED.name());
        assertThat(tasks.size()).isEqualTo(1);

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
        assertThat(tasks.size()).isEqualTo(0);

        // Checking that the process is finished
        processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                                                     HttpMethod.GET,
                                                     null,
                                                     new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                     },
                                                     "0",
                                                     "2");

        assertThat(processInstancesPage.getBody().getContent()).hasSize(0);
        assertThat(processInstancesPage.getBody().getMetadata().getTotalPages()).isEqualTo(0);

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
        WaitUtil.waitFor(streamHandler.getCompletedTaskAck(),
                         1000);
    }

    private void releaseTask(Task task) throws InterruptedException {
        ResponseEntity<PagedResources<Task>> responseEntity;
        Collection<Task> tasks;
        ReleaseTaskCmd releaseTaskCmd = new ReleaseTaskCmd(task.getId());
        String cmdId = UUID.randomUUID().toString();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(releaseTaskCmd).setHeader("cmdId",
                                                                                cmdId).build());
        WaitUtil.waitFor(streamHandler.getReleasedTaskAck(),
                         1000);
        responseEntity = getTasks();
        tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks).extracting(Task::getStatus).contains(Task.TaskStatus.CREATED.name());
        assertThat(tasks.size()).isEqualTo(1);
    }

    private void claimTask(Task task) throws InterruptedException {
        ResponseEntity<PagedResources<Task>> responseEntity;
        Collection<Task> tasks;
        ClaimTaskCmd claimTaskCmd = new ClaimTaskCmd(task.getId(),
                                                     "hruser");
        String cmdId = UUID.randomUUID().toString();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(claimTaskCmd).setHeader("cmdId",
                                                                              cmdId).build());

        WaitUtil.waitFor(streamHandler.getClaimedTaskAck(),
                         1000);

        responseEntity = getTasks();
        tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks).extracting(Task::getStatus).contains(Task.TaskStatus.ASSIGNED.name());
        assertThat(tasks.size()).isEqualTo(1);
    }

    private void activateProcessInstance(String processDefinitionId,
                                         String processInstanceId) throws InterruptedException {
        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPage;
        Collection<ProcessInstance> instances;//given
        ActivateProcessInstanceCmd activateProcessInstanceCmd = new ActivateProcessInstanceCmd(processInstanceId);
        String cmdId = UUID.randomUUID().toString();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(activateProcessInstanceCmd).setHeader("cmdId",
                                                                                            cmdId).build());

        WaitUtil.waitFor(streamHandler.getActivatedProcessInstanceAck(),
                         1000);
        //when
        processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                                                     HttpMethod.GET,
                                                     null,
                                                     new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                     },
                                                     "0",
                                                     "2");

        //then
        assertThat(processInstancesPage).isNotNull();
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody().getContent().size()).isEqualTo(1);

        instances = processInstancesPage.getBody().getContent();
        ProcessInstance instance = instances.iterator().next();
        assertThat(instance.getProcessDefinitionId()).isEqualTo(processDefinitionId);
        assertThat(instance.getId()).isNotNull();
        assertThat(instance.getStartDate()).isNotNull();
        assertThat(instance.getStatus()).isEqualToIgnoringCase(ProcessInstance.ProcessInstanceStatus.RUNNING.name());
    }

    private void suspendProcessInstance(String processDefinitionid) throws InterruptedException {
        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPage;
        Collection<ProcessInstance> instances;//given

        clientStream.myCmdProducer().send(MessageBuilder.withPayload(suspendProcessInstanceCmd).build());

        WaitUtil.waitFor(streamHandler.getSuspendedProcessInstanceAck(),
                         3000);
        //when
        processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                                                     HttpMethod.GET,
                                                     null,
                                                     new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                     },
                                                     "0",
                                                     "2");

        //then
        assertThat(processInstancesPage).isNotNull();
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody().getContent().size()).isEqualTo(1);

        instances = processInstancesPage.getBody().getContent();
        ProcessInstance instance = instances.iterator().next();
        assertThat(instance.getProcessDefinitionId()).isEqualTo(processDefinitionid);
        assertThat(instance.getId()).isNotNull();
        assertThat(instance.getStartDate()).isNotNull();
        assertThat(instance.getStatus()).isEqualToIgnoringCase(ProcessInstance.ProcessInstanceStatus.SUSPENDED.name());
    }

    private String startProcessInstance(String processDefinitionId,
                                        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPageBefore) throws InterruptedException {
        //given
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(startProcessInstanceCmd).build());

        WaitUtil.waitFor(streamHandler.getStartedProcessInstanceAck(),
                         3000);

        //when
        ResponseEntity<PagedResources<ProcessInstance>> processInstancesPage = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                                                                                                     HttpMethod.GET,
                                                                                                     null,
                                                                                                     new ParameterizedTypeReference<PagedResources<ProcessInstance>>() {
                                                                                                     },
                                                                                                     "0",
                                                                                                     "2");

        //then
        assertThat(processInstancesPage).isNotNull();
        assertThat(processInstancesPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processInstancesPage.getBody().getContent().size()).isEqualTo(1);

        Collection<ProcessInstance> instances = processInstancesPage.getBody().getContent();
        ProcessInstance instance = instances.iterator().next();

        assertThat(instance.getProcessDefinitionId()).isEqualTo(processDefinitionId);
        assertThat(instance.getId()).isNotNull();
        assertThat(instance.getStartDate()).isNotNull();
        assertThat(instance.getStatus()).isEqualToIgnoringCase(ProcessInstance.ProcessInstanceStatus.RUNNING.name());

        //should have only started one
        assertThat(processInstancesPage.getBody().getContent().size() - processInstancesPageBefore.getBody().getContent().size()).isEqualTo(1);

        //expecting we started with none
        assertThat(processInstancesPageBefore.getBody().getContent()).hasSize(0);

        assertThat(processInstancesPage.getBody().getContent()).hasSize(1);
        assertThat(processInstancesPage.getBody().getMetadata().getTotalPages()).isGreaterThanOrEqualTo(1);
        return instance.getId();
    }

    private ResponseEntity<PagedResources<Task>> getTasks() {
        return restTemplate.exchange(TASKS_URL,
                                     HttpMethod.GET,
                                     null,
                                     PAGED_TASKS_RESPONSE_TYPE);
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
