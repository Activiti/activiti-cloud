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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationErrorReceivedEventImpl;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationErrorReceivedEventHandlerTest {

    private static final String INTEGRATION_CONTEXT_ID = "integration-context-id";
    private static final String PROCESS_INSTANCE_ID = "process-instance-id";
    private static final String CLIENT_ID = "client-id";
    private static final String EXECUTION_ID = "execution-id";
    private static final String SERVICE_TASK_ID = PROCESS_INSTANCE_ID + ":" + CLIENT_ID + ":" + EXECUTION_ID;

    private static final String ERROR_CODE = "500";
    private static final String ERROR_MESSAGE = "Connector invocation failed";
    private static final String ERROR_CLASS_NAME = "java.io.IOException";

    @InjectMocks
    private IntegrationErrorReceivedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    void should_keepInBoundVariablesOfExistingEntity_when_errorEventCarriesNoInBoundVariables() {
        IntegrationContextImpl integrationContext = buildIntegrationContext();
        integrationContext.addOutBoundVariables(Map.of("out", "error-payload"));

        IntegrationContextEntity existingEntity = buildIntegrationContextEntity();
        existingEntity.setInBoundVariables(Map.of("in", "requested-value"));

        ServiceTaskEntity serviceTaskEntity = buildServiceTaskEntity();

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(existingEntity);
        when(entityManager.find(ServiceTaskEntity.class, SERVICE_TASK_ID)).thenReturn(serviceTaskEntity);

        handler.handle(buildErrorEvent(integrationContext, List.of()));

        verify(entityManager).persist(existingEntity);
        assertThat(existingEntity.getInBoundVariables()).containsExactlyEntriesOf(Map.of("in", "requested-value"));
        assertThat(existingEntity.getOutBoundVariables()).containsExactlyEntriesOf(Map.of("out", "error-payload"));
        assertThat(existingEntity.getStatus()).isEqualTo(IntegrationContextStatus.INTEGRATION_ERROR_RECEIVED);
        assertThat(existingEntity.getErrorCode()).isEqualTo(ERROR_CODE);
        assertThat(existingEntity.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(existingEntity.getErrorClassName()).isEqualTo(ERROR_CLASS_NAME);
        assertThat(existingEntity.getErrorDate()).isNotNull();

        verify(entityManager).persist(serviceTaskEntity);
        assertThat(serviceTaskEntity.getStatus()).isEqualTo(BPMNActivityStatus.ERROR);
    }

    @Test
    void should_notOverwriteInBoundVariablesOfExistingEntity_when_errorEventCarriesInBoundVariables() {
        IntegrationContextImpl integrationContext = buildIntegrationContext();
        integrationContext.addInBoundVariables(Map.of("in", "stale-value"));

        IntegrationContextEntity existingEntity = buildIntegrationContextEntity();
        existingEntity.setInBoundVariables(Map.of("in", "requested-value"));

        ServiceTaskEntity serviceTaskEntity = buildServiceTaskEntity();

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(existingEntity);
        when(entityManager.find(ServiceTaskEntity.class, SERVICE_TASK_ID)).thenReturn(serviceTaskEntity);

        handler.handle(buildErrorEvent(integrationContext, List.of()));

        verify(entityManager).persist(existingEntity);
        assertThat(existingEntity.getInBoundVariables()).containsExactlyEntriesOf(Map.of("in", "requested-value"));
    }

    @Test
    void should_addFullErrorMessageAsFirstStackTraceElement_when_eventHasStackTrace() {
        IntegrationContextImpl integrationContext = buildIntegrationContext();
        StackTraceElement eventElement = new StackTraceElement("MyConnector", "invoke", "MyConnector.java", 42);

        IntegrationContextEntity existingEntity = buildIntegrationContextEntity();
        ServiceTaskEntity serviceTaskEntity = buildServiceTaskEntity();

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(existingEntity);
        when(entityManager.find(ServiceTaskEntity.class, SERVICE_TASK_ID)).thenReturn(serviceTaskEntity);

        handler.handle(buildErrorEvent(integrationContext, List.of(eventElement)));

        assertThat(existingEntity.getStackTraceElements())
            .extracting(StackTraceElement::getClassName, StackTraceElement::getFileName)
            .containsExactly(tuple(ERROR_MESSAGE, "MyConnector.java"), tuple("MyConnector", "MyConnector.java"));
    }

    @Test
    void should_createEntityWithInBoundVariablesFromEvent_when_entityDoesNotExist() {
        IntegrationContextImpl integrationContext = buildIntegrationContext();
        integrationContext.addInBoundVariables(Map.of("in", "recovered-value"));
        integrationContext.addOutBoundVariables(Map.of("out", "error-payload"));

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(null);
        when(entityManager.find(ServiceTaskEntity.class, SERVICE_TASK_ID)).thenReturn(null);

        handler.handle(buildErrorEvent(integrationContext, List.of()));

        ArgumentCaptor<IntegrationContextEntity> captor = ArgumentCaptor.forClass(IntegrationContextEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue().getInBoundVariables()).containsExactlyEntriesOf(Map.of("in", "recovered-value"));
        assertThat(captor.getValue().getOutBoundVariables()).containsExactlyEntriesOf(Map.of("out", "error-payload"));
    }

    private IntegrationContextImpl buildIntegrationContext() {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(INTEGRATION_CONTEXT_ID);
        integrationContext.setProcessInstanceId(PROCESS_INSTANCE_ID);
        integrationContext.setClientId(CLIENT_ID);
        integrationContext.setExecutionId(EXECUTION_ID);
        return integrationContext;
    }

    private IntegrationContextEntity buildIntegrationContextEntity() {
        IntegrationContextEntity entity = new IntegrationContextEntity(
            "service-name",
            "service-full-name",
            "service-version",
            "app-name",
            "app-version"
        );
        entity.setId(INTEGRATION_CONTEXT_ID);
        return entity;
    }

    private ServiceTaskEntity buildServiceTaskEntity() {
        return new ServiceTaskEntity("service-name", "service-full-name", "service-version", "app-name", "app-version");
    }

    private CloudIntegrationErrorReceivedEventImpl buildErrorEvent(
        IntegrationContextImpl integrationContext,
        List<StackTraceElement> stackTraceElements
    ) {
        return new CloudIntegrationErrorReceivedEventImpl(
            "event-id",
            System.currentTimeMillis(),
            integrationContext,
            ERROR_CODE,
            ERROR_MESSAGE,
            ERROR_CLASS_NAME,
            stackTraceElements
        );
    }
}
