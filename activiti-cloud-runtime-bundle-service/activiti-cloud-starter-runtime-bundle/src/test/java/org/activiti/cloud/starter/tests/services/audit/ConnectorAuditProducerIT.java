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

import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_COMPLETED;
import static org.activiti.api.process.model.events.BPMNActivityEvent.ActivityEvents.ACTIVITY_STARTED;
import static org.activiti.api.process.model.events.BPMNErrorReceivedEvent.ErrorEvents.ERROR_RECEIVED;
import static org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents.INTEGRATION_ERROR_RECEIVED;
import static org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents.INTEGRATION_REQUESTED;
import static org.activiti.api.process.model.events.IntegrationEvent.IntegrationEvents.INTEGRATION_RESULT_RECEIVED;
import static org.activiti.api.process.model.events.ProcessRuntimeEvent.ProcessEvents.PROCESS_COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.api.model.shared.event.RuntimeEvent;
import org.activiti.api.process.model.BPMNActivity;
import org.activiti.api.process.model.BPMNError;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.CloudBpmnError;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityCompletedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNActivityStartedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
import org.activiti.cloud.api.process.model.impl.IntegrationErrorImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationRequestImpl;
import org.activiti.cloud.api.process.model.impl.IntegrationResultImpl;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TaskRestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles(ConnectorAuditProducerIT.AUDIT_PRODUCER_IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ServicesAuditITConfiguration.class, initializers = { RabbitMQContainerApplicationInitializer.class, KeycloakContainerApplicationInitializer.class})
public class ConnectorAuditProducerIT {

    private static final String ROUTING_KEY_HEADER = "routingKey";
    private static final String[] RUNTIME_BUNDLE_INFO_HEADERS = {"appName", "serviceName", "serviceVersion", "serviceFullName", ROUTING_KEY_HEADER};
    public static final String[] ALL_REQUIRED_HEADERS = Stream.of(RUNTIME_BUNDLE_INFO_HEADERS)
        .flatMap(Stream::of)
        .toArray(String[]::new);

    public static final String AUDIT_PRODUCER_IT = "AuditProducerIT";

    @Value("${activiti.keycloak.test-user}")
    protected String keycloakTestUser;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    @Autowired
    private TaskRestTemplate taskRestTemplate;

    @Autowired
    private AuditConsumerStreamHandler streamHandler;

    @Autowired
    private BinderAwareChannelResolver channelResolver;

    @Value("integrationResult_${spring.application.name}")
    private String integrationResultDestination;

    @Value("integrationError_${spring.application.name}")
    private String integrationErrorDestination;

    @BeforeEach
    public void setUp() {
        streamHandler.clear();
    }

    @Test
    public void shouldProduceIntegrationResultEventsDuringMultiInstanceCloudConnectorExecution() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("miParallelCloudConnector",
            Collections.singletonMap("instanceCount", 3),
            null);

        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudBPMNActivityStartedEvent> receivedActivityStartedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == ACTIVITY_STARTED && event.getEntityId()
                        .equals("miCloudConnectorId"))
                    .map(CloudBPMNActivityStartedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedActivityStartedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((BPMNActivity) event.getEntity()).getElementId())
                    .containsExactlyInAnyOrder(
                        tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                        tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                        tuple(ACTIVITY_STARTED, "miCloudConnectorId")
                    );

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_REQUESTED &&
                        ((IntegrationContext) event.getEntity()).getClientId()
                            .equals("miCloudConnectorId"))
                    .map(CloudIntegrationRequestedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                        tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                        tuple(INTEGRATION_REQUESTED, "miCloudConnectorId")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        sendIntegrationResultFor(integrationRequestedEvents);

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudBPMNActivityCompletedEvent> receivedActivityCompletedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == ACTIVITY_COMPLETED && event.getEntityId()
                        .equals("miCloudConnectorId"))
                    .map(CloudBPMNActivityCompletedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedActivityCompletedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((BPMNActivity) event.getEntity()).getElementId())
                    .containsExactlyInAnyOrder(
                        tuple(ACTIVITY_COMPLETED, "miCloudConnectorId"),
                        tuple(ACTIVITY_COMPLETED, "miCloudConnectorId"),
                        tuple(ACTIVITY_COMPLETED, "miCloudConnectorId")
                    );

                List<CloudIntegrationResultReceivedEvent> receivedIntegrationResultEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_RESULT_RECEIVED &&
                        ((IntegrationContext) event.getEntity()).getClientId()
                            .equals("miCloudConnectorId"))
                    .map(CloudIntegrationResultReceivedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_RESULT_RECEIVED, "miCloudConnectorId"),
                        tuple(INTEGRATION_RESULT_RECEIVED, "miCloudConnectorId"),
                        tuple(INTEGRATION_RESULT_RECEIVED, "miCloudConnectorId")
                    );

                assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(PROCESS_COMPLETED, startProcessEntity.getBody().getId()));
            });
    }

    @Test
    public void shouldProduceIntegrationErrorEventsDuringMultiInstanceCloudConnectorExecution() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("miParallelCloudConnector",
            Collections.singletonMap("instanceCount", 3),
            null);
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudBPMNActivityStartedEvent> receivedActivityStartedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == ACTIVITY_STARTED && event.getEntityId()
                        .equals("miCloudConnectorId"))
                    .map(CloudBPMNActivityStartedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedActivityStartedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((BPMNActivity) event.getEntity()).getElementId())
                    .containsExactlyInAnyOrder(
                        tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                        tuple(ACTIVITY_STARTED, "miCloudConnectorId"),
                        tuple(ACTIVITY_STARTED, "miCloudConnectorId")
                    );

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_REQUESTED &&
                        ((IntegrationContext) event.getEntity()).getClientId()
                            .equals("miCloudConnectorId"))
                    .map(CloudIntegrationRequestedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                        tuple(INTEGRATION_REQUESTED, "miCloudConnectorId"),
                        tuple(INTEGRATION_REQUESTED, "miCloudConnectorId")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        MessageChannel resultsChannel = channelResolver.resolveDestination(integrationErrorDestination);

        Error error = new Error("IntegrationError");
        error.fillInStackTrace();

        // throw error in cloud connector
        integrationRequestedEvents.stream()
            .map(request -> {
                return new IntegrationErrorImpl(new IntegrationRequestImpl(request.getEntity()),
                    error);
            })
            .map(payload -> MessageBuilder.withPayload(payload)
                .build())
            .forEach(resultsChannel::send);
        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudIntegrationErrorReceivedEvent> receivedIntegrationResultEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == INTEGRATION_ERROR_RECEIVED &&
                        ((IntegrationContext) event.getEntity()).getClientId()
                            .equals("miCloudConnectorId"))
                    .map(CloudIntegrationErrorReceivedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_ERROR_RECEIVED, "miCloudConnectorId"),
                        tuple(INTEGRATION_ERROR_RECEIVED, "miCloudConnectorId"),
                        tuple(INTEGRATION_ERROR_RECEIVED, "miCloudConnectorId")
                    );
            });
    }

    @Test
    public void shouldProduceIntegrationCloudBpmnErrorEventsForCloudBpmnErrorConnectorProcess() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("cloudBpmnErrorCloudConnectorProcess",
            null,
            null);
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = getEventsByType(receivedEvents,
                    INTEGRATION_REQUESTED);

                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "performBusinessTaskCloudConnector")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        sendIntegrationErrorFor(integrationRequestedEvents);
        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudIntegrationErrorReceivedEvent> receivedIntegrationResultEvents = getEventsByType(receivedEvents,
                    INTEGRATION_ERROR_RECEIVED);
                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_ERROR_RECEIVED, "performBusinessTaskCloudConnector")
                    );

                List<CloudBPMNErrorReceivedEvent> receivedBmpnErrorEvents = receivedEvents.stream()
                    .filter(event -> event.getEventType() == ERROR_RECEIVED)
                    .map(CloudBPMNErrorReceivedEvent.class::cast)
                    .collect(Collectors.toList());

                assertThat(receivedBmpnErrorEvents)
                    .extracting(CloudBPMNErrorReceivedEvent::getEventType,
                        event -> ((BPMNError) event.getEntity()).getErrorCode())
                    .containsExactlyInAnyOrder(
                        tuple(ERROR_RECEIVED,
                            "CLOUD_BPMN_ERROR")
                    );
            });

        // given reset state
        integrationRequestedEvents.clear();
        streamHandler.clear();

        // when fix business error
        List<CloudTask> tasks = new ArrayList<>(processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent());
        assertThat(tasks).hasSize(1);

        taskRestTemplate.complete(tasks.get(0));

        // then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = getEventsByType(receivedEvents,
                    INTEGRATION_REQUESTED);
                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "performBusinessTaskCloudConnector")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        sendIntegrationResultFor(integrationRequestedEvents);

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudIntegrationResultReceivedEvent> receivedIntegrationResultEvents = getEventsByType(receivedEvents,
                    INTEGRATION_RESULT_RECEIVED);
                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_RESULT_RECEIVED, "performBusinessTaskCloudConnector")
                    );

                assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(PROCESS_COMPLETED, startProcessEntity.getBody().getId()));
            });
    }

    @Test
    public void shouldProduceIntegrationCloudBpmnErrorEventsForCloudBpmnTerminateEndEventProcess() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("cloudBpmnErrorEndEventProcess",
            null,
            null);
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = getEventsByType(receivedEvents,
                    INTEGRATION_REQUESTED);
                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "performBusinessTaskCloudConnector2")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        sendIntegrationErrorFor(integrationRequestedEvents);
        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudIntegrationErrorReceivedEvent> receivedIntegrationResultEvents = getEventsByType(receivedEvents,
                    INTEGRATION_ERROR_RECEIVED);
                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_ERROR_RECEIVED, "performBusinessTaskCloudConnector2")
                    );

                List<CloudBPMNErrorReceivedEvent> receivedBmpnErrorEvents = getEventsByType(receivedEvents,
                    ERROR_RECEIVED);
                assertThat(receivedBmpnErrorEvents)
                    .extracting(CloudBPMNErrorReceivedEvent::getEventType,
                        event -> ((BPMNError) event.getEntity()).getErrorCode())
                    .containsExactlyInAnyOrder(
                        tuple(ERROR_RECEIVED,
                            "CLOUD_BPMN_ERROR")
                    );
            });

        // given reset state
        integrationRequestedEvents.clear();
        streamHandler.clear();

        // when fix business error
        List<CloudTask> tasks = new ArrayList<>(processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent());
        assertThat(tasks).hasSize(1);

        taskRestTemplate.complete(tasks.get(0));

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED, startProcessEntity.getBody().getId()));
            });
    }

    @Test
    public void shouldProduceIntegrationCloudBpmnErrorEventsForCloudBpmnErrorBoundarySubprocessEventProcess() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("cloudBpmnErrorBoundarySubprocessEventProcess",
            null,
            null);
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = getEventsByType(receivedEvents,
                    INTEGRATION_REQUESTED);
                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "performBusinessTaskCloudConnector4")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        sendIntegrationErrorFor(integrationRequestedEvents);
        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudIntegrationErrorReceivedEvent> receivedIntegrationResultEvents = getEventsByType(receivedEvents,
                    INTEGRATION_ERROR_RECEIVED);
                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_ERROR_RECEIVED, "performBusinessTaskCloudConnector4")
                    );

                List<CloudBPMNErrorReceivedEvent> receivedBmpnErrorEvents = getEventsByType(receivedEvents,
                    ERROR_RECEIVED);
                assertThat(receivedBmpnErrorEvents)
                    .extracting(CloudBPMNErrorReceivedEvent::getEventType,
                        event -> ((BPMNError) event.getEntity()).getErrorCode())
                    .containsExactlyInAnyOrder(
                        tuple(ERROR_RECEIVED,
                            "CLOUD_BPMN_ERROR")
                    );
            });

        // given reset state
        integrationRequestedEvents.clear();
        streamHandler.clear();

        // when fix business error
        List<CloudTask> tasks = new ArrayList<>(processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent());
        assertThat(tasks).hasSize(1);

        taskRestTemplate.complete(tasks.get(0));

        // then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = getEventsByType(receivedEvents,
                    INTEGRATION_REQUESTED);
                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "performBusinessTaskCloudConnector4")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        sendIntegrationResultFor(integrationRequestedEvents);

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudIntegrationResultReceivedEvent> receivedIntegrationResultEvents = getEventsByType(receivedEvents,
                    INTEGRATION_RESULT_RECEIVED);
                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_RESULT_RECEIVED, "performBusinessTaskCloudConnector4")
                    );

                assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(PROCESS_COMPLETED, startProcessEntity.getBody().getId()));
            });
    }

    private void sendIntegrationResultFor(
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents) {
        MessageChannel resultsChannel = channelResolver
            .resolveDestination(integrationResultDestination);

        // complete cloud connector tasks
        integrationRequestedEvents.stream()
            .map(request -> {
                return new IntegrationResultImpl(new IntegrationRequestImpl(request.getEntity()),
                    request.getEntity());
            })
            .map(payload -> MessageBuilder.withPayload(payload)
                .build())
            .forEach(resultsChannel::send);
    }

    @Test
    public void shouldProduceIntegrationCloudBpmnErrorEventsForCloudBpmnErrorEventSubprocessProcess() {

        //when
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcessByKey("cloudBpmnErrorEventSubprocessProcess",
            null,
            null);
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents = new ArrayList<>();

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(streamHandler.getReceivedHeaders()).containsKeys(ALL_REQUIRED_HEADERS);

                List<CloudIntegrationRequestedEvent> receivedIntegrationRequestedEvents = getEventsByType(receivedEvents,
                    INTEGRATION_REQUESTED);
                assertThat(receivedIntegrationRequestedEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_REQUESTED, "performBusinessTaskCloudConnector3")
                    );

                integrationRequestedEvents.addAll(receivedIntegrationRequestedEvents);

            });

        sendIntegrationErrorFor(integrationRequestedEvents);
        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                List<CloudIntegrationErrorReceivedEvent> receivedIntegrationResultEvents = getEventsByType(receivedEvents,
                    INTEGRATION_ERROR_RECEIVED);
                assertThat(receivedIntegrationResultEvents)
                    .extracting(RuntimeEvent::getEventType, event -> ((IntegrationContext) event.getEntity()).getClientId())
                    .containsExactlyInAnyOrder(
                        tuple(INTEGRATION_ERROR_RECEIVED, "performBusinessTaskCloudConnector3")
                    );

                List<CloudBPMNErrorReceivedEvent> receivedBmpnErrorEvents = getEventsByType(receivedEvents,
                    ERROR_RECEIVED);
                assertThat(receivedBmpnErrorEvents)
                    .extracting(CloudBPMNErrorReceivedEvent::getEventType,
                        event -> ((BPMNError) event.getEntity()).getErrorCode())
                    .containsExactlyInAnyOrder(
                        tuple(ERROR_RECEIVED,
                            "CLOUD_BPMN_ERROR")
                    );
            });

        // given reset state
        integrationRequestedEvents.clear();
        streamHandler.clear();

        // when fix business error
        List<CloudTask> tasks = new ArrayList<>(processInstanceRestTemplate.getTasks(startProcessEntity).getBody().getContent());
        assertThat(tasks).hasSize(1);

        taskRestTemplate.complete(tasks.get(0));

        //then
        await()
            .untilAsserted(() -> {
                List<CloudRuntimeEvent<?, ?>> receivedEvents = getProcessInstanceEvents(startProcessEntity);

                assertThat(receivedEvents).extracting(RuntimeEvent::getEventType,
                    RuntimeEvent::getProcessInstanceId)
                    .contains(tuple(PROCESS_COMPLETED, startProcessEntity.getBody().getId()));
            });
    }

    private void sendIntegrationErrorFor(
        List<CloudIntegrationRequestedEvent> integrationRequestedEvents) {
        CloudBpmnError error = new CloudBpmnError("CLOUD_BPMN_ERROR");

        MessageChannel errorChannel = channelResolver
            .resolveDestination(integrationErrorDestination);

        integrationRequestedEvents.stream()
            .map(request -> {
                return new IntegrationErrorImpl(new IntegrationRequestImpl(request.getEntity()),
                    error);
            })
            .map(payload -> MessageBuilder.withPayload(payload)
                .build())
            .forEach(errorChannel::send);
    }

    private List<CloudRuntimeEvent<?, ?>> getProcessInstanceEvents(ResponseEntity<CloudProcessInstance> processInstanceEntity) {
        return streamHandler.getAllReceivedEvents()
            .stream()
            .filter(event -> processInstanceEntity.getBody().getId().equals(event.getProcessInstanceId()))
            .collect(Collectors.toList());
    }

    private <T> List<T> getEventsByType(List<CloudRuntimeEvent<?, ?>> receivedEvents,
        Enum<?> eventType) {
        return receivedEvents.stream()
            .filter(event -> event.getEventType() == eventType)
            .map(it -> (T) it)
            .collect(Collectors.toList());
    }

}
