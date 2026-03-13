/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.events.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.IncidentContext;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.activiti.cloud.api.process.model.impl.events.CloudIncidentCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.TestUtils;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.EventChunker;
import org.activiti.cloud.services.events.message.ExecutionContextIncidentEventMessageBuilderFactory;
import org.activiti.cloud.services.events.message.ExecutionContextMessageBuilderFactory;
import org.activiti.cloud.services.events.services.IncidentService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageProducerCommandContextCloseListenerTest {

    private static final String MOCK_ROUTING_KEY = "engineEvents.springAppName.appName";
    private static final String LORG_ACTIVITI_CLOUD_API_MODEL_SHARED_EVENTS_CLOUD_RUNTIME_EVENT =
        "[Lorg.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;";
    private static final String SPRING_APP_NAME = "springAppName";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String APP_NAME = "appName";

    private MessageProducerCommandContextCloseListener closeListener;

    @Mock
    private ProcessEngineChannels producer;

    @Mock
    private MessageChannel auditChannel;

    @Mock
    private MessageChannel auditIncidentsChannel;

    @Mock
    private CommandContext commandContext;

    @Spy
    private RuntimeBundleProperties properties = new RuntimeBundleProperties() {
        {
            setAppName(APP_NAME);
            setServiceType(SERVICE_TYPE);
            setServiceVersion(SERVICE_VERSION);
            setRbSpringAppName(SPRING_APP_NAME);
            getEventsProperties().setChunkSizeInBytesCloseListener(3000);
        }
    };

    @Spy
    private EventChunker eventChunker = new EventChunker(new ObjectMapper(), properties);

    @Spy
    private ExecutionContextMessageBuilderFactory messageBuilderChainFactory = new ExecutionContextMessageBuilderFactory(
        properties
    );

    @Spy
    private ExecutionContextIncidentEventMessageBuilderFactory messageBuilderChainIncidentFactory = new ExecutionContextIncidentEventMessageBuilderFactory(
        properties
    );

    private ProcessEngineEventsAggregator processEngineEventsAggregator;

    @Spy
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender = new RuntimeBundleInfoAppender(properties);

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ManagementService managementService;

    private IncidentService incidentService;

    @Captor
    private ArgumentCaptor<Message<CloudRuntimeEvent<?, ?>[]>> messageArgumentCaptor;

    @Captor
    private ArgumentCaptor<Message<List<CloudRuntimeEvent<?, ?>>>> incidentMessageArgumentCaptor;

    private CloudRuntimeEventImpl<?, ?> event;

    @BeforeEach
    public void setUp() throws Exception {
        incidentService =
            new IncidentService(
                producer,
                messageBuilderChainIncidentFactory,
                runtimeBundleInfoAppender,
                managementService,
                runtimeService
            );

        closeListener =
            new MessageProducerCommandContextCloseListener(
                producer,
                messageBuilderChainFactory,
                runtimeBundleInfoAppender,
                properties,
                eventChunker,
                incidentService
            );

        ProcessInstance processInstance = new ProcessInstanceImpl();
        event = new CloudProcessCreatedEventImpl(processInstance);

        when(producer.auditProducer()).thenReturn(auditChannel);
        when(producer.auditProducerIncidents()).thenReturn(auditIncidentsChannel);

        processEngineEventsAggregator = spy(new ProcessEngineEventsAggregator(closeListener));

        when(processEngineEventsAggregator.getCurrentCommandContext()).thenReturn(commandContext);

        ExecutionContext executionContext = TestUtils.mockExecutionContext();
        given(commandContext.getGenericAttribute(event.getEntityId())).willReturn(executionContext);
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT))
            .willReturn(executionContext);
    }

    @Test
    void closedShouldSendEventsRegisteredOnTheCommandContext() {
        // given
        processEngineEventsAggregator.add(event);
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(Collections.singletonList(event));

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditChannel).send(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().getPayload()).containsExactly(event);

        CloudRuntimeEvent<?, ?>[] result = messageArgumentCaptor.getValue().getPayload();

        assertThat(result).hasSize(1);

        assertThat(result[0].getProcessInstanceId()).isEqualTo(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        assertThat(result[0].getParentProcessInstanceId()).isEqualTo(TestUtils.MOCK_PARENT_PROCESS_INSTANCE_ID);
        assertThat(result[0].getBusinessKey()).isEqualTo(TestUtils.MOCK_BUSINESS_KEY);
        assertThat(result[0].getProcessDefinitionId()).isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_ID);
        assertThat(result[0].getProcessDefinitionKey()).isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_KEY);
        assertThat(result[0].getProcessDefinitionVersion()).isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_VERSION);

        assertThat(result[0].getAppName()).isEqualTo(APP_NAME);
        assertThat(result[0].getServiceName()).isEqualTo(SPRING_APP_NAME);
        assertThat(result[0].getServiceType()).isEqualTo(SERVICE_TYPE);
        assertThat(result[0].getServiceVersion()).isEqualTo(SERVICE_VERSION);
        assertThat(result[0].getActor()).isEqualTo("service_user");
    }

    @Test
    void closedShouldDoNothingWhenRegisteredEventsIsNull() {
        // given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(null);

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditChannel, never()).send(any());
    }

    @Test
    void closedShouldDoNothingWhenRegisteredEventsIsEmpty() {
        // given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(Collections.emptyList());

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditChannel, never()).send(any());
    }

    @Test
    void closedShouldSendMessageHeadersWithExecutionContext() {
        // given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(Collections.singletonList(event));

        // when
        closeListener.closed(commandContext);

        // then
        verify(auditIncidentsChannel, never()).send(any());
        verify(auditChannel).send(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().getHeaders())
            .containsEntry("routingKey", MOCK_ROUTING_KEY)
            .containsEntry("messagePayloadType", LORG_ACTIVITI_CLOUD_API_MODEL_SHARED_EVENTS_CLOUD_RUNTIME_EVENT)
            .containsEntry("appName", APP_NAME)
            .containsEntry("serviceName", SPRING_APP_NAME)
            .containsEntry("serviceType", SERVICE_TYPE)
            .containsEntry("serviceVersion", SERVICE_VERSION)
            .containsEntry("serviceFullName", SPRING_APP_NAME);
    }

    @Test
    void closedShouldSendEventsInChunksWhenMultipleEventsExist() {
        List<CloudRuntimeEventImpl<?, ?>> events = getCloudRuntimeEvents(8);

        given(this.commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(events);

        this.closeListener.closed(this.commandContext);

        verify(this.auditChannel, times(3)).send(this.messageArgumentCaptor.capture());

        List<Message<CloudRuntimeEvent<?, ?>[]>> capturedMessages = this.messageArgumentCaptor.getAllValues();

        assertThat(capturedMessages.getFirst().getPayload()).hasSize(3);
        assertThat(capturedMessages.get(1).getPayload()).hasSize(3);
        assertThat(capturedMessages.get(2).getPayload()).hasSize(2);

        for (Message<CloudRuntimeEvent<?, ?>[]> message : capturedMessages) {
            CloudRuntimeEvent<?, ?>[] payload = message.getPayload();
            for (CloudRuntimeEvent<?, ?> event : payload) {
                assertThat(event.getAppName()).isEqualTo(APP_NAME);
                assertThat(event.getServiceName()).isEqualTo(SPRING_APP_NAME);
                assertThat(event.getServiceType()).isEqualTo(SERVICE_TYPE);
                assertThat(event.getServiceVersion()).isEqualTo(SERVICE_VERSION);
            }
        }
    }

    @Test
    void closedShouldSendSingleMessageWhenEventsAreLessThanChunkSize() {
        List<CloudRuntimeEventImpl<?, ?>> events = getCloudRuntimeEvents(2);

        given(this.commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(events);

        this.closeListener.closed(this.commandContext);

        verify(this.auditChannel, times(1)).send(this.messageArgumentCaptor.capture());

        CloudRuntimeEvent<?, ?>[] payload = this.messageArgumentCaptor.getValue().getPayload();
        assertThat(payload).hasSize(2);

        for (CloudRuntimeEvent<?, ?> event : payload) {
            assertThat(event.getAppName()).isEqualTo(APP_NAME);
            assertThat(event.getServiceName()).isEqualTo(SPRING_APP_NAME);
        }
    }

    @Test
    void closedShouldSendSingleMessageWhenChunkSizeIsZero() {
        var testListener = getMessageProducerCloseListenerWithDisabledChunker();
        List<CloudRuntimeEventImpl<?, ?>> events = getCloudRuntimeEvents(10);

        given(this.commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(events);

        testListener.closed(this.commandContext);

        verify(this.auditChannel, times(1)).send(this.messageArgumentCaptor.capture());

        var payload = this.messageArgumentCaptor.getValue().getPayload();
        assertThat(payload).hasSize(10);
    }

    @Test
    void closedShouldNotSendMessageWhenSingleEventIsLargerThanLimit() {
        List<CloudRuntimeEventImpl<?, ?>> events = getLargeCloudRuntimeEvents(1);

        given(this.commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(events);

        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> this.closeListener.closed(this.commandContext)
        );

        assertMessageNotSent(exception);
    }

    @Test
    void closedShouldNotSendMessageWhenTwoEventAreLargerThatLimit() {
        List<CloudRuntimeEventImpl<?, ?>> events = getLargeCloudRuntimeEvents(2);

        given(this.commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(events);

        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> this.closeListener.closed(this.commandContext)
        );

        assertMessageNotSent(exception);
    }

    @Test
    void closedShouldSendIncidentMessageWithCorrectContentWhenChunkSizeLimitExceeded() {
        var incidentContext = mock(IncidentContext.class);
        when(incidentContext.getProcessInstanceId()).thenReturn(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        when(incidentContext.getProcessDefinitionId()).thenReturn(TestUtils.MOCK_PROCESS_DEFINITION_ID);
        when(incidentContext.getExecutionId()).thenReturn(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        var incidentCreatedEvent = new CloudIncidentCreatedEventImpl(
            new IllegalArgumentException("Chunk size limit exceeded"),
            incidentContext,
            IncidentSeverity.ERROR
        );

        var message = MessageBuilder.withPayload(List.of(incidentCreatedEvent)).build();
        when(managementService.executeCommand(any())).thenReturn(message);

        List<CloudRuntimeEventImpl<?, ?>> events = getLargeCloudRuntimeEvents(1);

        given(this.commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(events);

        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> this.closeListener.closed(this.commandContext)
        );
        assertThat(exception).hasMessage("Chunk size limit exceeded");

        verify(auditChannel, never()).send(any());
        verify(auditIncidentsChannel).send(incidentMessageArgumentCaptor.capture());

        List<CloudRuntimeEvent<?, ?>> incidentPayload = incidentMessageArgumentCaptor.getValue().getPayload();
        assertThat(incidentPayload).hasSize(1);

        CloudIncidentCreatedEventImpl incident = (CloudIncidentCreatedEventImpl) incidentPayload.getFirst();
        assertThat(incident.getErrorClassName()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(incident.getErrorMessage()).contains("Chunk size limit exceeded");
        assertThat(incident.getEntity().getProcessInstanceId()).isEqualTo(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        assertThat(incident.getEntity().getProcessDefinitionId()).isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_ID);
        assertThat(incident.getEntity().getExecutionId()).isEqualTo(TestUtils.MOCK_PROCESS_INSTANCE_ID);
    }

    private MessageProducerCommandContextCloseListener getMessageProducerCloseListenerWithDisabledChunker() {
        var runtimeBundleProperties = new RuntimeBundleProperties() {
            {
                setAppName(APP_NAME);
                setServiceType(SERVICE_TYPE);
                setServiceVersion(SERVICE_VERSION);
                setRbSpringAppName(SPRING_APP_NAME);
                getEventsProperties().setChunkSizeInBytesCloseListener(0);
            }
        };

        return new MessageProducerCommandContextCloseListener(
            producer,
            messageBuilderChainFactory,
            runtimeBundleInfoAppender,
            runtimeBundleProperties,
            eventChunker,
            incidentService
        );
    }

    private void assertMessageNotSent(RuntimeException exception) {
        verify(this.auditChannel, never()).send(any());
        verify(auditIncidentsChannel).send(any());
        assertThat(exception).hasMessage("Chunk size limit exceeded");
        assertThat(exception.getClass()).isEqualTo(IllegalArgumentException.class);
    }

    private List<CloudRuntimeEventImpl<?, ?>> getCloudRuntimeEvents(int eventsCount) {
        List<CloudRuntimeEventImpl<?, ?>> events = new ArrayList<>();
        for (int i = 0; i < eventsCount; i++) {
            ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
            processInstance.setId(TestUtils.MOCK_PROCESS_INSTANCE_ID + "_" + i);
            CloudProcessCreatedEventImpl event = new CloudProcessCreatedEventImpl(processInstance);
            events.add(event);
        }
        return events;
    }

    private List<CloudRuntimeEventImpl<?, ?>> getLargeCloudRuntimeEvents(int eventsCount) {
        List<CloudRuntimeEventImpl<?, ?>> events = new ArrayList<>();
        for (int i = 0; i < eventsCount; i++) {
            ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
            StringBuilder largeData = new StringBuilder("LARGE_DATA_");
            for (int j = 0; j < 500; j++) {
                largeData.append("This_is_large_test_data_to_exceed_bytes_limit_");
            }
            processInstance.setId(TestUtils.MOCK_PROCESS_INSTANCE_ID + "_" + largeData + "_" + i);
            processInstance.setBusinessKey(largeData.toString());
            CloudProcessCreatedEventImpl event = new CloudProcessCreatedEventImpl(processInstance);
            events.add(event);
        }
        return events;
    }
}
