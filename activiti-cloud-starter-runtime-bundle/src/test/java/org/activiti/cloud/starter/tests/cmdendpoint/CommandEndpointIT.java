
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.activiti.cloud.services.test.identity.keycloak.interceptor.KeycloakSecurityContextClientRequestInterceptor;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.runtime.api.model.CloudProcessDefinition;
import org.activiti.runtime.api.model.CloudProcessInstance;
import org.activiti.runtime.api.model.CloudTask;
import org.activiti.runtime.api.model.CloudVariableInstance;
import org.activiti.runtime.api.model.ProcessDefinition;
import org.activiti.runtime.api.model.ProcessInstance;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.VariableInstance;
import org.activiti.runtime.api.model.builders.ProcessPayloadBuilder;
import org.activiti.runtime.api.model.builders.TaskPayloadBuilder;
import org.activiti.runtime.api.model.payloads.ClaimTaskPayload;
import org.activiti.runtime.api.model.payloads.CompleteTaskPayload;
import org.activiti.runtime.api.model.payloads.ReleaseTaskPayload;
import org.activiti.runtime.api.model.payloads.RemoveProcessVariablesPayload;
import org.activiti.runtime.api.model.payloads.ResumeProcessPayload;
import org.activiti.runtime.api.model.payloads.SetProcessVariablesPayload;
import org.activiti.runtime.api.model.payloads.SetTaskVariablesPayload;
import org.activiti.runtime.api.model.payloads.SignalPayload;
import org.activiti.runtime.api.model.payloads.StartProcessPayload;
import org.activiti.runtime.api.model.payloads.SuspendProcessPayload;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.activiti.runtime.api.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.runtime.api.model.Task.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private CommandEndPointITStreamHandler streamHandler;

    private Map<String, String> processDefinitionIds = new HashMap<>();

    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";
    private static final String PROCESS_INSTANCES_RELATIVE_URL = "/v1/process-instances/";
    private static final String TASKS_URL = "/v1/tasks/";

    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final String SIGNAL_PROCESS = "ProcessWithBoundarySignal";

    @Autowired
    private KeycloakSecurityContextClientRequestInterceptor keycloakSecurityContextClientRequestInterceptor;

    @Before
    public void setUp() {
        keycloakSecurityContextClientRequestInterceptor.setKeycloakTestUser("hruser");

        // Get Available Process Definitions
        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
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
        StartProcessPayload startProcessInstanceCmd = ProcessPayloadBuilder.start().withProcessDefinitionId(simpleProcessDefinitionId).withVariables(vars).build();

        String processInstanceId = startProcessInstance(startProcessInstanceCmd);

        SuspendProcessPayload suspendProcessInstanceCmd = ProcessPayloadBuilder.suspend(processInstanceId);
        suspendProcessInstance(suspendProcessInstanceCmd);

        resumeProcessInstance(simpleProcessDefinitionId,
                              processInstanceId);

        // Get Tasks

        //when
        ResponseEntity<PagedResources<CloudTask>> responseEntity = getTasks(processInstanceId);

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks).extracting(Task::getStatus).contains(CREATED);

        Task task = tasks.iterator().next();

        setTaskVariables(task);

        setAndRemoveProcessVariables(processInstanceId);

        claimTask(task);

        releaseTask(task);

        claimTask(task);

        completeTask(task);

        responseEntity = getTasks(processInstanceId);
        tasks = responseEntity.getBody().getContent();
        assertThat(tasks)
                .filteredOn(t -> t.getId().equals(task.getId()))
                .isEmpty();

        Thread.sleep(1000);
        await().untilAsserted(() -> {
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
        });

        assertThat(streamHandler.getStartedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getSuspendedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getActivatedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getClaimedTaskAck()).isTrue();
        assertThat(streamHandler.getReleasedTaskAck()).isTrue();
        assertThat(streamHandler.getCompletedTaskAck()).isTrue();
    }

    private void completeTask(Task task) {
        Map<String, Object> variables = new HashMap<>();

        CompleteTaskPayload completeTaskCmd = TaskPayloadBuilder.complete().withTaskId(task.getId()).withVariables(
                variables).build();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(completeTaskCmd).setHeader("cmdId",
                                                                                                completeTaskCmd.getId()).build());
        await("task to be completed").untilTrue(streamHandler.getCompletedTaskAck());
    }

    private void releaseTask(Task task) {
        ReleaseTaskPayload releaseTaskCmd = TaskPayloadBuilder.release().withTaskId(task.getId()).build();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(releaseTaskCmd).setHeader("cmdId",
                                                                                               releaseTaskCmd.getId()).build());
        await("task to be released").untilTrue(streamHandler.getReleasedTaskAck());

        assertThatTaskHasStatus(task.getId(),
                                CREATED);
    }

    private void setTaskVariables(Task task) {
        Map<String, Object> variables = Collections.singletonMap("taskVar",
                                                                 "v1");
        SetTaskVariablesPayload setTaskVariables = TaskPayloadBuilder.setVariables().withTaskId(task.getId()).withVariables(
                variables).build();

        clientStream.myCmdProducer().send(MessageBuilder.withPayload(setTaskVariables).setHeader("cmdId",
                                                                                                 setTaskVariables.getId()).build());

        await("Variable to be set").untilTrue(streamHandler.getSetTaskVariablesAck());

        ResponseEntity<Resources<CloudVariableInstance>> retrievedVars = taskRestTemplate.getVariables(task.getId());
        assertThat(retrievedVars.getBody().getContent())
                .extracting(VariableInstance::getName,
                            VariableInstance::getValue)
                .contains(tuple("taskVar",
                                "v1"));
    }

    private void setAndRemoveProcessVariables(String proInstanceId) {
        Map<String, Object> variables = Collections.singletonMap("procVar",
                                                                 "v2");
        SetProcessVariablesPayload setProcessVariables = ProcessPayloadBuilder.setVariables().withProcessInstanceId(proInstanceId).withVariables(variables).build();

        clientStream.myCmdProducer().send(MessageBuilder.withPayload(setProcessVariables).setHeader("cmdId",
                                                                                                    setProcessVariables.getId()).build());

        await("Variable to be set").untilTrue(streamHandler.getSetProcessVariablesAck());

        ResponseEntity<Resources<CloudVariableInstance>> retrievedVars = processInstanceRestTemplate.getVariables(proInstanceId);
        assertThat(retrievedVars.getBody().getContent())
                .extracting(VariableInstance::getName,
                            VariableInstance::getValue)
                .contains(tuple("procVar",
                                "v2"));

        RemoveProcessVariablesPayload removeProcessVariables = ProcessPayloadBuilder.removeVariables()
                .withProcessInstanceId(proInstanceId)
                .withVariableNames(Collections.singletonList("procVar")).build();

        clientStream.myCmdProducer().send(MessageBuilder.withPayload(removeProcessVariables).setHeader("cmdId",
                                                                                                       removeProcessVariables.getId()).build());

        await("Variable to be removed").untilTrue(streamHandler.getRemoveProcessVariablesAck());

        retrievedVars = processInstanceRestTemplate.getVariables(proInstanceId);
        assertThat(retrievedVars.getBody().getContent())
                .extracting(VariableInstance::getName,
                            VariableInstance::getValue)
                .doesNotContain(tuple("procVar",
                                      "v2"));
    }

    private void claimTask(Task task) {
        ClaimTaskPayload claimTaskPayload = TaskPayloadBuilder.claim().withTaskId(task.getId()).withAssignee("hruser").build();
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(claimTaskPayload).setHeader("cmdId",
                                                                                                 claimTaskPayload.getId()).build());

        await("task to be claimed").untilTrue(streamHandler.getClaimedTaskAck());

        assertThatTaskHasStatus(task.getId(),
                                ASSIGNED
        );
    }

    private void assertThatTaskHasStatus(String taskId,
                                         Task.TaskStatus status) {
        ResponseEntity<CloudTask> responseEntity = getTask(taskId);
        Task retrievedTask = responseEntity.getBody();
        assertThat(retrievedTask.getStatus()).isEqualTo(status);
    }

    private void resumeProcessInstance(String processDefinitionId,
                                       String processInstanceId) {
        //given
        ResumeProcessPayload resumeProcess = ProcessPayloadBuilder.resume(processInstanceId);
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(resumeProcess).setHeader("cmdId",
                                                                                              resumeProcess.getId()).build());

        await("process to be activated").untilTrue(streamHandler.getActivatedProcessInstanceAck());

        await().untilAsserted(() -> {

            //when
            ProcessInstance processInstance = executeGetProcessInstanceRequest(processInstanceId);

            //then

            assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinitionId);
            assertThat(processInstance.getId()).isNotNull();
            assertThat(processInstance.getStartDate()).isNotNull();
            assertThat(processInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
        });
    }

    private void suspendProcessInstance(SuspendProcessPayload suspendProcessInstanceCmd) {
        //given

        clientStream.myCmdProducer().send(MessageBuilder.withPayload(suspendProcessInstanceCmd).build());

        await("process to be suspended").untilTrue(streamHandler.getSuspendedProcessInstanceAck());
        //when
        ProcessInstance processInstance = executeGetProcessInstanceRequest(suspendProcessInstanceCmd.getProcessInstanceId());

        //then
        assertThat(processInstance.getId()).isEqualTo(suspendProcessInstanceCmd.getProcessInstanceId());
        assertThat(processInstance.getStartDate()).isNotNull();
        assertThat(processInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.SUSPENDED);
    }

    private String startProcessInstance(StartProcessPayload startProcessPayload) {
        //given
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(startProcessPayload).build());

        await("process to be started")
                .untilTrue(streamHandler.getStartedProcessInstanceAck());
        String processInstanceId = streamHandler.getProcessInstanceId();

        //when
        ProcessInstance processInstance = executeGetProcessInstanceRequest(processInstanceId);

        //then
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(startProcessPayload.getProcessDefinitionId());
        assertThat(processInstance.getId()).isNotNull();
        assertThat(processInstance.getStartDate()).isNotNull();
        assertThat(processInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
        return processInstance.getId();
    }

    private ProcessInstance executeGetProcessInstanceRequest(String processInstanceId) {
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "{processInstanceId}",
                                                                                                   HttpMethod.GET,
                                                                                                   null,
                                                                                                   new ParameterizedTypeReference<CloudProcessInstance>() {
                                                                                                   },
                                                                                                   processInstanceId);

        assertThat(processInstanceResponseEntity).isNotNull();
        assertThat(processInstanceResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        ProcessInstance processInstance = processInstanceResponseEntity.getBody();
        assertThat(processInstance).isNotNull();
        return processInstance;
    }

    private ResponseEntity<PagedResources<CloudTask>> getTasks(String processInstanceId) {
        return processInstanceRestTemplate.getTasks(processInstanceId);
    }

    private ResponseEntity<CloudTask> getTask(String taskId) {
        ResponseEntity<CloudTask> responseEntity = restTemplate.exchange(TASKS_URL + taskId,
                                                                         HttpMethod.GET,
                                                                         null,
                                                                         new ParameterizedTypeReference<CloudTask>() {
                                                                         });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }

    @Test
    public void shouldSendSignalViaCommand() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIGNAL_PROCESS));
        SignalPayload sendSignal = ProcessPayloadBuilder.signal().withName("go").build();

        //when
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(sendSignal).setHeader("cmdId",
                                                                                           sendSignal.getId()).build());
        //then
        await("signal to be sent")
                .untilTrue(streamHandler.getSendSignalAck());

        ResponseEntity<PagedResources<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");
    }
}
