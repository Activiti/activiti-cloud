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
package org.activiti.cloud.starter.tests.services.audit;

import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_CANCELLED;
import static org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents.MESSAGE_RECEIVED;
import static org.activiti.api.process.model.events.BPMNMessageEvent.MessageEvents.MESSAGE_WAITING;
import static org.activiti.api.process.model.events.BPMNSignalEvent.SignalEvents.SIGNAL_RECEIVED;
import static org.activiti.api.process.model.events.MessageSubscriptionEvent.MessageSubscriptionEvents.MESSAGE_SUBSCRIPTION_CANCELLED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.*;
import static org.activiti.api.process.model.events.SequenceFlowEvent.SequenceFlowEvents.SEQUENCE_FLOW_TAKEN;
import static org.activiti.api.task.model.events.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_ADDED;
import static org.activiti.api.task.model.events.TaskCandidateGroupEvent.TaskCandidateGroupEvents.TASK_CANDIDATE_GROUP_REMOVED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_CREATED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_COMPLETED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_CANCELLED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_ASSIGNED;
import static org.activiti.api.task.model.events.TaskRuntimeEvent.TaskEvents.TASK_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.api.task.model.events.CloudTaskAssignedEvent;
import org.activiti.cloud.api.task.model.events.CloudTaskCreatedEvent;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.SignalRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(AuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
@ContextConfiguration(classes = ServicesAuditITConfiguration.class, initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class EmbeddedSubProcessAuditIT {

    private static final String SIMPLE_SUB_PROCESS1 = "simpleSubProcess1";
    private static final String SIMPLE_EMBEDDED_SUB_PROCESS = "startSimpleSubProcess";
    private static final String SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY = "startSimpleSubProcessWithCallActivity";
    private static final String SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT = "signalSubProcess";
    private static final String INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS = "messageInterruptingSubProcess";
    private static final String NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS = "messageNonInterruptingSubProcess";

    private static final String ROUTING_KEY_HEADER = "routingKey";
    private static final String[] RUNTIME_BUNDLE_INFO_HEADERS = {"appName", "serviceName",
                                                                 "serviceVersion", "serviceFullName", ROUTING_KEY_HEADER};
    private static final String[] ALL_REQUIRED_HEADERS = Stream.of(RUNTIME_BUNDLE_INFO_HEADERS)
                                                                 .flatMap(Stream::of)
                                                                 .toArray(String[]::new);

    private static final String PROCESS_DEFINITIONS_URL = "/v1/process-definitions/";

    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private SignalRestTemplate signalRestTemplate;


    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Autowired
    private RuntimeService runtimeService;

    @Test
    public void shouldExecuteProcessWithEmbeddedSubProcess() {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate
                                                                       .startProcess(buildStartProcessPayload(SIMPLE_EMBEDDED_SUB_PROCESS));

        String processInstanceId = processInstance.getBody().getId();

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(
                            tuple(PROCESS_CREATED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(PROCESS_UPDATED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(PROCESS_STARTED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                  SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                  SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(TASK_CANDIDATE_GROUP_ADDED, null, null, null),
                            tuple(TASK_CREATED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS)
                                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> TASK_CREATED.equals(event.getEventType()))
                    .extracting(CloudRuntimeEvent::getProcessInstanceId,
                                event -> ((CloudTaskCreatedEvent) event).getEntity().getProcessInstanceId())
                    .containsExactly(tuple(processInstanceId, processInstanceId));

            List<CloudRuntimeEvent<?, ?>> activitiStartedEvents = receivedEvents.stream()
                                                                          .filter(event -> ACTIVITY_STARTED.equals(event.getEventType()))
                                                                          .collect(Collectors.toList());

            assertThat(activitiStartedEvents)
                    .filteredOn(event -> ACTIVITY_STARTED.equals(event.getEventType()))
                    .extracting(
                            event -> ((CloudBPMNActivityStartedEvent) event).getEntity().getActivityType(),
                            event -> ((CloudBPMNActivityStartedEvent) event).getEntity().getActivityName())
                    .containsExactly(tuple("startEvent", null),
                                     tuple("subProcess", "subProcess"),
                                     tuple("startEvent", null),
                                     tuple("userTask", "Task in subprocess"));


        });

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate
                                                                  .getTasks(processInstance);
        Task task = tasks.getBody().iterator().next();

        //when
        taskRestTemplate.claim(task);

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey
                               )
                    .containsExactly(
                            tuple(TASK_ASSIGNED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(TASK_UPDATED, processInstanceId, null, SIMPLE_EMBEDDED_SUB_PROCESS));

            String entityProcessInstanceId = ((CloudTaskAssignedEvent) receivedEvents.get(0))
                                                     .getEntity().getProcessInstanceId();
            assertThat(entityProcessInstanceId).isNotNull();
            assertThat(entityProcessInstanceId).isEqualTo(processInstanceId);

        });

        //when
        taskRestTemplate.complete(task);

        //then
        await().untilAsserted(() -> {
            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(
                            tuple(TASK_COMPLETED, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(TASK_CANDIDATE_GROUP_REMOVED, null, null),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId,
                                  SIMPLE_EMBEDDED_SUB_PROCESS)/*subProcess*/,
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS),
                            tuple(PROCESS_COMPLETED, processInstanceId, SIMPLE_EMBEDDED_SUB_PROCESS));

            CloudBPMNActivityCompletedEvent subprocessCompletedEvent = (CloudBPMNActivityCompletedEvent) receivedEvents
                                                                                                                 .stream()
                                                                                                                 .filter(event -> ACTIVITY_COMPLETED.equals(event.getEventType()) &&
                                                                                                                                  "subProcess".equals(
                                                                                                                                          ((CloudBPMNActivityCompletedEvent) event).getEntity().getActivityType()))
                                                                                                                 .collect(Collectors.toList())
                                                                                                                 .get(0);

            assertThat(subprocessCompletedEvent).isNotNull();
            assertThat(subprocessCompletedEvent.getProcessInstanceId())
                    .isEqualTo(processInstanceId);
        });

    }


    @Test
    public void shouldExecuteProcessWithEmbeddedSubProcessContainingCallActivity() {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate
                                                                       .startProcess(buildStartProcessPayload(SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY));

        String processInstanceId = processInstance.getBody().getId();

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(tuple(PROCESS_CREATED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(PROCESS_UPDATED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(PROCESS_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(TASK_CANDIDATE_GROUP_ADDED, null, null, null),
                                     tuple(TASK_CREATED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY)
                                    );

            assertThat(receivedEvents)
                    .filteredOn(event -> ACTIVITY_STARTED.equals(event.getEventType()))
                    .extracting(
                            event -> ((CloudBPMNActivityStartedEvent) event).getEntity().getActivityType(),
                            event -> ((CloudBPMNActivityStartedEvent) event).getEntity().getActivityName()
                               )
                    .containsExactly(tuple("startEvent", null),
                                     tuple("subProcess", "subProcess"),
                                     tuple("startEvent", null),
                                     tuple("userTask", "Task in subprocess")
                                    );


        });

        ResponseEntity<PagedModel<CloudTask>> tasks = processInstanceRestTemplate
                                                                  .getTasks(processInstance);
        Task task = tasks.getBody().iterator().next();

        String subProcessInstanceId = task.getProcessInstanceId();
        assertThat(subProcessInstanceId).isNotNull();
        assertThat(subProcessInstanceId).isEqualTo(processInstanceId);

        //when
        taskRestTemplate.claim(task);

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey
                               )
                    .containsExactly(tuple(TASK_ASSIGNED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(TASK_UPDATED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY));
        });

        //when
        taskRestTemplate.complete(task);

        //Check we have two processes and one of them is callActivity process
        // when

        ResponseEntity<PagedModel<ProcessInstance>> processes = processInstanceRestTemplate
                                                                            .getSubprocesses(processInstanceId);

        assertThat(processes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(processes.getBody()).isNotNull();
        assertThat(processes.getBody().getContent().size()).isEqualTo(1);

        assertThat(processes.getBody().getContent().iterator().next().getProcessDefinitionKey())
                .isEqualTo(SIMPLE_SUB_PROCESS1);
        String callActivityProcessId = processes.getBody().getContent().iterator().next().getId();

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey
                               )
                    .containsExactly(tuple(TASK_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(TASK_CANDIDATE_GROUP_REMOVED, null, null, null),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_CALLACTIVITY),
                                     tuple(PROCESS_CREATED, callActivityProcessId, processInstanceId,
                                           SIMPLE_SUB_PROCESS1),
                                     tuple(PROCESS_STARTED, callActivityProcessId, processInstanceId,
                                           SIMPLE_SUB_PROCESS1),
                                     tuple(ACTIVITY_STARTED, callActivityProcessId, processInstanceId,
                                           SIMPLE_SUB_PROCESS1),
                                     tuple(ACTIVITY_COMPLETED, callActivityProcessId, processInstanceId,
                                           SIMPLE_SUB_PROCESS1),
                                     tuple(SEQUENCE_FLOW_TAKEN, callActivityProcessId, processInstanceId,
                                           SIMPLE_SUB_PROCESS1),
                                     tuple(ACTIVITY_STARTED, callActivityProcessId, processInstanceId,
                                           SIMPLE_SUB_PROCESS1),
                                     tuple(TASK_CREATED, callActivityProcessId, processInstanceId,
                                           SIMPLE_SUB_PROCESS1)
                                    );


        });

        // Clean up
        runtimeService.deleteProcessInstance(callActivityProcessId, "Clean up");
        runtimeService.deleteProcessInstance(processInstanceId, "Clean up");

    }

    @Test
    public void shouldExecuteProcessWithEmbeddedSubProcessContainingSignalIntermediateCatchEvent() {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate
                                                                       .startProcess(buildStartProcessPayload(SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT));

        String processInstanceId = processInstance.getBody().getId();

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();
            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(tuple(PROCESS_CREATED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(PROCESS_UPDATED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(PROCESS_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(TASK_CREATED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT)
                                    );
        });

        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder
                                                          .signal()
                                                          .withName("mySignal")
                                                          .build();

        //when
        signalRestTemplate.signal(signalProcessInstancesCmd);

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(tuple(SIGNAL_RECEIVED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           SIMPLE_EMBEDDED_SUB_PROCESS_WITH_SIGNAL_EVENT)
                                    );
        });

        runtimeService.deleteProcessInstance(processInstanceId, "Clean up");
    }

    @Test
    public void shouldExecuteProcessWithMessageInterruptedEventSubProcess() {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate
                                                                       .startProcess(buildStartProcessPayload(INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS));

        String processInstanceId = processInstance.getBody().getId();

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();
            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(tuple(PROCESS_CREATED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(PROCESS_UPDATED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(PROCESS_STARTED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(MESSAGE_WAITING, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(TASK_CREATED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(TASK_ASSIGNED, processInstanceId, null,
                                           INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS)
                                    );
        });

        Execution executionWithMessage = runtimeService.createExecutionQuery()
                                                 .messageEventSubscriptionName("messageName").singleResult();
        assertThat(executionWithMessage).isNotNull();

        // event-subprocess received interrupted message event
        runtimeService.messageEventReceived("messageName", executionWithMessage.getId());

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(
                            tuple(MESSAGE_RECEIVED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_CANCELLED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(TASK_CANCELLED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(MESSAGE_SUBSCRIPTION_CANCELLED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(PROCESS_COMPLETED, processInstanceId, null,
                                  INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS)
                                    );
        });


    }

    @Test
    public void shouldExecuteProcessWithMessageNonInterruptedEventSubProcess() {
        //given
        ResponseEntity<CloudProcessInstance> processInstance = processInstanceRestTemplate
                                                                       .startProcess(buildStartProcessPayload(NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS));

        String processInstanceId = processInstance.getBody().getId();

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();
            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(tuple(PROCESS_CREATED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(PROCESS_UPDATED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(PROCESS_STARTED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(MESSAGE_WAITING, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(ACTIVITY_STARTED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(TASK_CREATED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                                     tuple(TASK_ASSIGNED, processInstanceId, null,
                                           NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS)
                                    );
        });

        Execution executionWithMessage = runtimeService.createExecutionQuery()
                                                 .messageEventSubscriptionName("messageName").singleResult();
        assertThat(executionWithMessage).isNotNull();

        // event-subprocess received non interrupted message event
        runtimeService.messageEventReceived("messageName", executionWithMessage.getId());

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(
                            tuple(MESSAGE_RECEIVED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(MESSAGE_SUBSCRIPTION_CANCELLED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS)
                                    );
        });

        Collection<CloudTask> tasks = processInstanceRestTemplate.getTasks(processInstance)
                                              .getBody().getContent();
        assertThat(tasks.size()).isEqualTo(1);

        taskRestTemplate.complete((Task) tasks.toArray()[0]);

        await().untilAsserted(() -> {

            assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);
            List<CloudRuntimeEvent<?, ?>> receivedEvents = streamHandler.getLatestReceivedEvents();

            assertThat(receivedEvents)
                    .extracting(CloudRuntimeEvent::getEventType,
                                CloudRuntimeEvent::getProcessInstanceId,
                                CloudRuntimeEvent::getParentProcessInstanceId,
                                CloudRuntimeEvent::getProcessDefinitionKey)
                    .containsExactly(
                            tuple(TASK_COMPLETED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(SEQUENCE_FLOW_TAKEN, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_STARTED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(ACTIVITY_COMPLETED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS),
                            tuple(PROCESS_COMPLETED, processInstanceId, null,
                                  NON_INTERRUPTING_MESSAGE_EVENT_SUB_PROCESS)
                                    );
        });

    }

    private ResponseEntity<PagedModel<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedModel<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedModel<CloudProcessDefinition>>() {
        };

        return restTemplate.exchange(PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }

    private StartProcessPayload buildStartProcessPayload(String processDefinitionKey) {
        return ProcessPayloadBuilder
                       .start()
                       .withProcessDefinitionKey(processDefinitionKey)
                       .build();
    }

}
