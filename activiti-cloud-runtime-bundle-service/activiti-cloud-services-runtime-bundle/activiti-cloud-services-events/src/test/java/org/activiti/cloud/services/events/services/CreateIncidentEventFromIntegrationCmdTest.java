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
package org.activiti.cloud.services.events.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.cloud.api.process.model.IncidentSeverity;
import org.activiti.cloud.api.process.model.impl.events.CloudIncidentCreatedEventImpl;
import org.activiti.cloud.services.events.TestUtils;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.converter.RuntimeBundleInfoAppender;
import org.activiti.cloud.services.events.message.ExecutionContextIncidentEventMessageBuilderFactory;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.activiti.engine.runtime.ExecutionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class CreateIncidentEventFromIntegrationCmdTest {

    private static final String SPRING_APP_NAME = "springAppName";
    private static final String SERVICE_VERSION = "serviceVersion";
    private static final String SERVICE_TYPE = "serviceType";
    private static final String APP_NAME = "appName";
    private static final String EXECUTION_ID = "myExecutionId";

    private CreateIncidentEventFromIntegrationCmd createIncidentEventFromIntegrationCmd;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

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

    private IllegalArgumentException testException = new IllegalArgumentException("Test exception");

    @Captor
    private ArgumentCaptor<ExecutionContext> executionContextArgumentCaptor;

    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    @BeforeEach
    void setUp() {
        var integrationContext = mock(IntegrationContext.class);
        lenient().when(integrationContext.getExecutionId()).thenReturn(EXECUTION_ID);

        this.createIncidentEventFromIntegrationCmd =
            spy(
                new CreateIncidentEventFromIntegrationCmd(
                    integrationContext,
                    this.testException,
                    this.runtimeService,
                    this.messageBuilderChainIncidentFactory,
                    this.runtimeBundleInfoAppender
                )
            );
    }

    @Test
    void shouldExecuteCreateIncidentEventFromIntegration() {
        var executionEntity = mockExecutionEntity();
        mockExecutionQuery(executionEntity);
        doReturn(null)
            .when(this.createIncidentEventFromIntegrationCmd)
            .createMessage(any(ExecutionContext.class), any());

        this.createIncidentEventFromIntegrationCmd.execute(null);

        verify(this.createIncidentEventFromIntegrationCmd)
            .createMessage(this.executionContextArgumentCaptor.capture(), this.exceptionCaptor.capture());

        assertThat(this.exceptionCaptor.getValue()).isEqualTo(testException);
        var executionContext = this.executionContextArgumentCaptor.getValue();
        assertThat(executionContext.getExecution().getProcessInstanceId())
            .isEqualTo(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        assertThat(executionContext.getExecution().getProcessDefinitionId())
            .isEqualTo(TestUtils.MOCK_PROCESS_DEFINITION_ID);
    }

    @Test
    void shouldCreateIncidentEventFromIntegration() {
        var executionContext = TestUtils.mockExecutionContext();

        Message<ArrayList<Object>> message =
            this.createIncidentEventFromIntegrationCmd.createMessage(executionContext, this.testException);

        var payload = (List) message.getPayload();
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

        assertThat(message.getHeaders())
            .contains(
                entry("routingKey", "engineEvents.springAppName.appName"),
                entry("messagePayloadType", "java.util.ArrayList")
            );
    }

    @Test
    void shouldDefaultSeverityToError() {
        var executionContext = TestUtils.mockExecutionContext();

        Message<ArrayList<Object>> message =
            this.createIncidentEventFromIntegrationCmd.createMessage(executionContext, this.testException);

        var incident = (CloudIncidentCreatedEventImpl) ((List) message.getPayload()).get(0);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.ERROR);
    }

    @Test
    void shouldCreateIncidentEventWithWarningSeverity() {
        var integrationContext = mock(IntegrationContext.class);
        lenient().when(integrationContext.getExecutionId()).thenReturn(EXECUTION_ID);

        var cmd = spy(
            new CreateIncidentEventFromIntegrationCmd(
                integrationContext,
                this.testException,
                this.runtimeService,
                this.messageBuilderChainIncidentFactory,
                this.runtimeBundleInfoAppender,
                IncidentSeverity.WARNING
            )
        );

        var executionContext = TestUtils.mockExecutionContext();

        Message<ArrayList<Object>> message = cmd.createMessage(executionContext, this.testException);

        var payload = (List) message.getPayload();
        assertThat(payload).hasSize(1);

        var incident = (CloudIncidentCreatedEventImpl) payload.get(0);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.WARNING);
        assertThat(incident.getErrorClassName()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(incident.getErrorMessage()).isEqualTo("Test exception");
    }

    private void mockExecutionQuery(ExecutionEntityImpl executionEntity) {
        var executionQuery = mock(ExecutionQuery.class);
        when(this.runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId(EXECUTION_ID)).thenReturn(executionQuery);
        when(executionQuery.list()).thenReturn(List.of(executionEntity));
    }

    private ExecutionEntityImpl mockExecutionEntity() {
        var executionEntity = mock(ExecutionEntityImpl.class);
        lenient().when(executionEntity.getId()).thenReturn(EXECUTION_ID);
        when(executionEntity.getProcessInstanceId()).thenReturn(TestUtils.MOCK_PROCESS_INSTANCE_ID);
        lenient().when(executionEntity.getProcessDefinitionId()).thenReturn(TestUtils.MOCK_PROCESS_DEFINITION_ID);
        return executionEntity;
    }
}
