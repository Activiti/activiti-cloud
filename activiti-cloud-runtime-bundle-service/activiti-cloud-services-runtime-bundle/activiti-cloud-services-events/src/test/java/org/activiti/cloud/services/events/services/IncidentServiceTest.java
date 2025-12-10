/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
package org.activiti.cloud.services.events.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.activiti.cloud.api.process.model.impl.events.CloudIncidentCreatedEventImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.TestUtils;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.ExecutionContextIncidentEventMessageBuilderFactory;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    private static final String SPRING_APP_NAME = "springAppName";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String APP_NAME = "appName";

    private IncidentService incidentService;

    @Mock
    private ProcessEngineChannels producer;

    @Mock
    private MessageChannel auditIncidentsChannel;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Mock
    private ManagementService managementService;

    @Mock
    private RuntimeService runtimeService;

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
    private ExecutionContextIncidentEventMessageBuilderFactory messageBuilderChainIncidentFactory = new ExecutionContextIncidentEventMessageBuilderFactory(
        properties
    );

    @Captor
    private ArgumentCaptor<Message<?>> messageArgumentCaptor;

    @BeforeEach
    void setUp() {
        when(this.producer.auditProducerIncidents()).thenReturn(this.auditIncidentsChannel);

        this.incidentService =
            new IncidentService(
                producer,
                messageBuilderChainIncidentFactory,
                runtimeBundleInfoAppender,
                managementService,
                runtimeService
            );
    }

    @Test
    void shouldCreateAndSendIncidentEvent() {
        var executionContext = TestUtils.mockExecutionContext();
        var exception = new IllegalArgumentException("Test exception");

        this.incidentService.createAndSendIncidentEvent(executionContext, exception);

        verify(this.producer.auditProducerIncidents()).send(this.messageArgumentCaptor.capture());

        Message<?> capturedMessage = this.messageArgumentCaptor.getValue();

        var payload = (List) capturedMessage.getPayload();
        assertThat(payload).hasSize(1);
        assertThat(payload.get(0)).isInstanceOf(CloudIncidentCreatedEventImpl.class);

        var incident = (CloudIncidentCreatedEventImpl) payload.get(0);
        assertThat(incident.getEntity().getProcessInstanceId()).isEqualTo(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        assertThat(incident.getEntity().getProcessDefinitionId()).isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_ID);
        assertThat(incident.getEntity().getExecutionId()).isEqualTo(TestUtils.MOCK_PROCESS_INSTANCE_ID);

        assertThat(incident.getErrorClassName()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(incident.getErrorMessage()).isEqualTo("Test exception");
        assertThat(incident.getStackTraceElements()).isNotNull();
        assertThat(incident.getStackTraceElements()).isNotEmpty();

        assertThat(incident.getProcessDefinitionKey()).isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_KEY);
        assertThat(incident.getProcessInstanceId()).isEqualTo(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        assertThat(incident.getProcessDefinitionId()).isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_ID);

        assertThat(capturedMessage.getHeaders())
            .contains(
                entry("routingKey", "engineEvents.springAppName.appName"),
                entry("messagePayloadType", "java.util.ArrayList")
            );
    }
}
