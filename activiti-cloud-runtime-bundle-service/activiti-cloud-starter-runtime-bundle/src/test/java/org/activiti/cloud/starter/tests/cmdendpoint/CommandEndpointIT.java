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
package org.activiti.cloud.starter.tests.cmdendpoint;

import static org.activiti.api.task.model.Task.TaskStatus.ASSIGNED;
import static org.activiti.api.task.model.Task.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.events.ProcessDeployedEvent;
import org.activiti.api.process.model.payloads.ResumeProcessPayload;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.SuspendProcessPayload;
import org.activiti.api.runtime.event.impl.ProcessDeployedEvents;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.ClaimTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.ReleaseTaskPayload;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.impl.SyncCloudProcessDefinitionsPayload;
import org.activiti.cloud.api.process.model.impl.SyncCloudProcessDefinitionsResult;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.test.containers.KeycloakContainerApplicationInitializer;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(CommandEndPointITStreamHandler.COMMAND_ENDPOINT_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@Import(
    {
        CommandEndPointITStreamHandler.class,
        ProcessInstanceRestTemplate.class,
        TaskRestTemplate.class,
        MessageClientStreamConfiguration.class,
        TestChannelBinderConfiguration.class,
        CommandEndpointIT.TestProcessDeployedEventsListener.class,
    }
)
@ContextConfiguration(initializers = { KeycloakContainerApplicationInitializer.class })
@DirtiesContext
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

    private final Map<String, String> processDefinitionIds = new HashMap<>();

    private final Map<String, String> processDefinitionKeys = new HashMap<>();

    private static final List<ProcessDeployedEvents> processDeployedEvents = new ArrayList<>();

    @TestComponent
    static class TestProcessDeployedEventsListener {

        @EventListener
        void on(ProcessDeployedEvents event) {
            processDeployedEvents.add(event);
        }
    }

    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions";
    private static final String PROCESS_INSTANCES_RELATIVE_URL = "/v1/process-instances";
    private static final String TASKS_URL = "/v1/tasks";

    private static final String SIMPLE_PROCESS = "SimpleProcess";
    private static final String SIGNAL_PROCESS = "ProcessWithBoundarySignal";

    @Autowired
    private IdentityTokenProducer identityTokenProducer;

    @BeforeEach
    public void setUp() {
        identityTokenProducer.withTestUser("hruser");

        // Get Available Process Definitions
        ResponseEntity<PagedModel<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getStatusCode()).isEqualTo(HttpStatus.OK);

        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(), pd.getId());
            processDefinitionKeys.put(pd.getKey(), pd.getId());
        }
    }

    @Test
    public void eventBasedStartProcessTests() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("hey", "one");

        String simpleProcessDefinitionId = processDefinitionIds.get(SIMPLE_PROCESS);
        StartProcessPayload startProcessInstanceCmd = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionId(simpleProcessDefinitionId)
            .withVariables(vars)
            .build();

        String processInstanceId = startProcessInstance(startProcessInstanceCmd);

        SuspendProcessPayload suspendProcessInstanceCmd = ProcessPayloadBuilder.suspend(processInstanceId);
        suspendProcessInstance(suspendProcessInstanceCmd);

        resumeProcessInstance(simpleProcessDefinitionId, processInstanceId);

        // Get Tasks

        //when
        ResponseEntity<PagedModel<CloudTask>> responseEntity = getTasks(processInstanceId);

        //then
        assertThat(responseEntity).isNotNull();
        Collection<CloudTask> tasks = responseEntity.getBody().getContent();
        assertThat(tasks).extracting(Task::getName).contains("Perform action");
        assertThat(tasks).extracting(Task::getStatus).contains(CREATED);

        Task task = tasks.iterator().next();

        setProcessVariables(processInstanceId);

        claimTask(task);

        releaseTask(task);

        claimTask(task);

        completeTask(task);

        responseEntity = getTasks(processInstanceId);
        tasks = responseEntity.getBody().getContent();
        assertThat(tasks).filteredOn(t -> t.getId().equals(task.getId())).isEmpty();

        Thread.sleep(1000);
        await()
            .untilAsserted(() -> {
                // Checking that the process is finished
                ResponseEntity<PagedModel<ProcessInstance>> processInstancesPage = restTemplate.exchange(
                    PROCESS_INSTANCES_RELATIVE_URL + "?page={page}&size={size}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<PagedModel<ProcessInstance>>() {},
                    "0",
                    "2"
                );

                assertThat(processInstancesPage.getBody().getContent())
                    .filteredOn(processInstance -> processInstance.getId().equals(processInstanceId))
                    .isEmpty();
            });

        assertThat(streamHandler.getStartedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getSuspendedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getResumedProcessInstanceAck()).isTrue();
        assertThat(streamHandler.getClaimedTaskAck()).isTrue();
        assertThat(streamHandler.getReleasedTaskAck()).isTrue();
        assertThat(streamHandler.getCompletedTaskAck()).isTrue();
    }

    @Test
    public void syncCloudProcessDefinitionsTest() {
        streamHandler.resetSyncProcessDefinitionsAck();
        processDeployedEvents.clear();
        var payload = new SyncCloudProcessDefinitionsPayload();

        var result = doSyncCloudProcessDefinitions(payload);

        assertThat(result).extracting(SyncCloudProcessDefinitionsResult::getPayload).isEqualTo(payload);

        assertThat(result)
            .extracting(SyncCloudProcessDefinitionsResult::getEntity)
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .contains(processDefinitionIds.values().toArray());

        assertThat(processDeployedEvents).isNotEmpty();

        assertThat(
            processDeployedEvents
                .stream()
                .flatMap(it -> it.getProcessDeployedEvents().stream())
                .map(ProcessDeployedEvent::getProcessDefinitionId)
                .toList()
        )
            .contains(processDefinitionIds.values().toArray(String[]::new));
    }

    protected SyncCloudProcessDefinitionsResult doSyncCloudProcessDefinitions(
        SyncCloudProcessDefinitionsPayload payload
    ) {
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(payload).setHeader("cmdId", "jobId").build());

        await("process definitions result to be synced")
            .untilAsserted(() -> assertThat(streamHandler.getSyncProcessDefinitionsAck()).isNotNull());

        return streamHandler.getSyncProcessDefinitionsAck().get();
    }

    @Test
    public void syncCloudProcessDefinitionsExcludedTest() {
        streamHandler.resetSyncProcessDefinitionsAck();

        var payload = SyncCloudProcessDefinitionsPayload
            .builder()
            .excludedProcessDefinitionIds(List.of(processDefinitionIds.values().toArray(String[]::new)))
            .build();

        var result = doSyncCloudProcessDefinitions(payload);

        assertThat(result).extracting(SyncCloudProcessDefinitionsResult::getPayload).isEqualTo(payload);

        assertThat(result)
            .extracting(SyncCloudProcessDefinitionsResult::getEntity)
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .isNotEmpty()
            .doesNotContain(processDefinitionIds.values().toArray());
    }

    @Test
    public void syncCloudProcessDefinitionsKeysTest() {
        final var testProcessDefinitionKey = "SimpleProcess";
        streamHandler.resetSyncProcessDefinitionsAck();

        var payload = SyncCloudProcessDefinitionsPayload
            .builder()
            .processDefinitionKeys(List.of(testProcessDefinitionKey))
            .excludedProcessDefinitionIds(List.of("foo", "bar"))
            .build();

        var result = doSyncCloudProcessDefinitions(payload);

        assertThat(result).extracting(SyncCloudProcessDefinitionsResult::getPayload).isEqualTo(payload);

        assertThat(result)
            .extracting(SyncCloudProcessDefinitionsResult::getEntity)
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .isNotEmpty()
            .satisfies(processDefinitionIds ->
                assertThat(
                    processDefinitionIds
                        .stream()
                        .map(String.class::cast)
                        .allMatch(it -> it.contains(testProcessDefinitionKey))
                )
                    .isTrue()
            );
    }

    private void completeTask(Task task) {
        Map<String, Object> variables = new HashMap<>();

        CompleteTaskPayload completeTaskCmd = TaskPayloadBuilder
            .complete()
            .withTaskId(task.getId())
            .withVariables(variables)
            .build();

        doCompleteTask(completeTaskCmd);
    }

    protected void doCompleteTask(CompleteTaskPayload payload) {
        clientStream
            .myCmdProducer()
            .send(MessageBuilder.withPayload(payload).setHeader("cmdId", payload.getId()).build());

        await("task to be completed").untilTrue(streamHandler.getCompletedTaskAck());
    }

    private void releaseTask(Task task) {
        ReleaseTaskPayload releaseTaskCmd = TaskPayloadBuilder.release().withTaskId(task.getId()).build();

        doReleaseTask(releaseTaskCmd);

        assertThatTaskHasStatus(task.getId(), CREATED);
    }

    protected void doReleaseTask(ReleaseTaskPayload payload) {
        clientStream
            .myCmdProducer()
            .send(MessageBuilder.withPayload(payload).setHeader("cmdId", payload.getId()).build());

        await("task to be released").untilTrue(streamHandler.getReleasedTaskAck());
    }

    private void setProcessVariables(String proInstanceId) {
        Map<String, Object> variables = Collections.singletonMap("procVar", "v2");
        SetProcessVariablesPayload setProcessVariables = ProcessPayloadBuilder
            .setVariables()
            .withProcessInstanceId(proInstanceId)
            .withVariables(variables)
            .build();

        doSetProcessVariables(setProcessVariables);

        ResponseEntity<CollectionModel<CloudVariableInstance>> retrievedVars = processInstanceRestTemplate.getVariables(
            proInstanceId
        );
        assertThat(retrievedVars.getBody().getContent())
            .extracting(VariableInstance::getName, VariableInstance::getValue)
            .contains(tuple("procVar", "v2"));
    }

    protected void doSetProcessVariables(SetProcessVariablesPayload payload) {
        clientStream
            .myCmdProducer()
            .send(MessageBuilder.withPayload(payload).setHeader("cmdId", payload.getId()).build());

        await("Variable to be set").untilTrue(streamHandler.getSetProcessVariablesAck());
    }

    private void claimTask(Task task) {
        streamHandler.resetClaimedTaskAck();
        ClaimTaskPayload claimTaskPayload = TaskPayloadBuilder
            .claim()
            .withTaskId(task.getId())
            .withAssignee("hruser")
            .build();

        doClaimTask(claimTaskPayload);

        assertThatTaskHasStatus(task.getId(), ASSIGNED);
    }

    protected void doClaimTask(ClaimTaskPayload claimTaskPayload) {
        clientStream
            .myCmdProducer()
            .send(MessageBuilder.withPayload(claimTaskPayload).setHeader("cmdId", claimTaskPayload.getId()).build());

        await("task to be claimed").untilTrue(streamHandler.getClaimedTaskAck());
    }

    private void assertThatTaskHasStatus(String taskId, Task.TaskStatus status) {
        ResponseEntity<CloudTask> responseEntity = getTask(taskId);
        Task retrievedTask = responseEntity.getBody();
        assertThat(retrievedTask.getStatus()).isEqualTo(status);
    }

    private void resumeProcessInstance(String processDefinitionId, String processInstanceId) {
        //given
        ResumeProcessPayload resumeProcess = ProcessPayloadBuilder.resume(processInstanceId);

        doResumeProcessInstance(resumeProcess);

        await()
            .untilAsserted(() -> {
                //when
                ProcessInstance processInstance = executeGetProcessInstanceRequest(processInstanceId);

                //then

                assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinitionId);
                assertThat(processInstance.getId()).isNotNull();
                assertThat(processInstance.getStartDate()).isNotNull();
                assertThat(processInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
            });
    }

    protected void doResumeProcessInstance(ResumeProcessPayload resumeProcess) {
        clientStream
            .myCmdProducer()
            .send(MessageBuilder.withPayload(resumeProcess).setHeader("cmdId", resumeProcess.getId()).build());

        await("process to be resumed").untilTrue(streamHandler.getResumedProcessInstanceAck());
    }

    private void suspendProcessInstance(SuspendProcessPayload suspendProcessInstanceCmd) {
        //given
        doSuspendProcessInstance(suspendProcessInstanceCmd);

        //when
        ProcessInstance processInstance = executeGetProcessInstanceRequest(
            suspendProcessInstanceCmd.getProcessInstanceId()
        );

        //then
        assertThat(processInstance.getId()).isEqualTo(suspendProcessInstanceCmd.getProcessInstanceId());
        assertThat(processInstance.getStartDate()).isNotNull();
        assertThat(processInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.SUSPENDED);
    }

    protected void doSuspendProcessInstance(SuspendProcessPayload payload) {
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(payload).build());

        await("process to be suspended").untilTrue(streamHandler.getSuspendedProcessInstanceAck());
    }

    private String startProcessInstance(StartProcessPayload startProcessPayload) {
        //given
        var processInstanceId = doStartProcessInstance(startProcessPayload);

        //when
        ProcessInstance processInstance = executeGetProcessInstanceRequest(processInstanceId);

        //then
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(startProcessPayload.getProcessDefinitionId());
        assertThat(processInstance.getId()).isNotNull();
        assertThat(processInstance.getStartDate()).isNotNull();
        assertThat(processInstance.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);
        return processInstance.getId();
    }

    protected String doStartProcessInstance(StartProcessPayload payload) {
        clientStream.myCmdProducer().send(MessageBuilder.withPayload(payload).build());

        await("process to be started").untilTrue(streamHandler.getStartedProcessInstanceAck());

        return streamHandler.getProcessInstanceId();
    }

    private ProcessInstance executeGetProcessInstanceRequest(String processInstanceId) {
        ResponseEntity<CloudProcessInstance> processInstanceResponseEntity = restTemplate.exchange(
            PROCESS_INSTANCES_RELATIVE_URL.concat("/").concat("{processInstanceId}"),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<CloudProcessInstance>() {},
            processInstanceId
        );

        assertThat(processInstanceResponseEntity).isNotNull();
        assertThat(processInstanceResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        ProcessInstance processInstance = processInstanceResponseEntity.getBody();
        assertThat(processInstance).isNotNull();
        return processInstance;
    }

    private ResponseEntity<PagedModel<CloudTask>> getTasks(String processInstanceId) {
        return processInstanceRestTemplate.getTasks(processInstanceId);
    }

    private ResponseEntity<CloudTask> getTask(String taskId) {
        ResponseEntity<CloudTask> responseEntity = restTemplate.exchange(
            TASKS_URL.concat("/").concat(taskId),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<CloudTask>() {}
        );
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        return responseEntity;
    }

    private ResponseEntity<PagedModel<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedModel<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedModel<CloudProcessDefinition>>() {};

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL, HttpMethod.GET, null, responseType);
    }

    @Test
    public void shouldSendSignalViaCommand() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(
            processDefinitionIds.get(SIGNAL_PROCESS)
        );
        SignalPayload sendSignal = ProcessPayloadBuilder.signal().withName("go").build();

        doSendSignal(sendSignal);

        ResponseEntity<PagedModel<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");
    }

    protected void doSendSignal(SignalPayload sendSignal) {
        //when
        clientStream
            .myCmdProducer()
            .send(MessageBuilder.withPayload(sendSignal).setHeader("cmdId", sendSignal.getId()).build());

        //then
        await("signal to be sent").untilTrue(streamHandler.getSendSignalAck());
    }
}
