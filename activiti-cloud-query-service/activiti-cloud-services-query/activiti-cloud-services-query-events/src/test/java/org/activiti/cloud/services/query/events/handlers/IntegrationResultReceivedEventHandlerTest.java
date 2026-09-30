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

import static org.activiti.cloud.api.events.CloudRuntimeEventType.INTEGRATION_RESULT_RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationResultReceivedEventImpl;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationResultReceivedEventHandlerTest {

    private static final String INTEGRATION_CONTEXT_ID = "integration-context-id";
    private static final String PROCESS_INSTANCE_ID = "process-instance-id";
    private static final String CLIENT_ID = "client-id";
    private static final String EXECUTION_ID = "execution-id";

    @InjectMocks
    private IntegrationResultReceivedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    void should_updateResultOfExistingEntity_when_entityExists() {
        IntegrationContextImpl resultIntegrationContext = buildIntegrationContext();
        resultIntegrationContext.addOutBoundVariables(Map.of("out", "result-payload"));

        IntegrationContextEntity existingEntity = buildIntegrationContextEntity();
        ServiceTaskEntity serviceTaskEntity = buildServiceTaskEntity();

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(existingEntity);
        when(
            entityManager.find(
                ServiceTaskEntity.class,
                IntegrationContextEntity.IdBuilderHelper.from(resultIntegrationContext)
            )
        ).thenReturn(serviceTaskEntity);

        handler.handle(buildResultEvent(resultIntegrationContext));

        verify(entityManager).persist(existingEntity);
        assertThat(existingEntity.getOutBoundVariables()).containsExactlyEntriesOf(Map.of("out", "result-payload"));
        assertThat(existingEntity.getStatus()).isEqualTo(IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED);
        assertThat(existingEntity.getResultDate()).isNotNull();
        assertThat(existingEntity.getServiceTask()).isSameAs(serviceTaskEntity);
        assertThat(serviceTaskEntity.getIntegrationContextCounter()).isZero();
    }

    @Test
    void should_keepInBoundVariablesOfExistingEntity_when_resultEventCarriesInBoundVariables() {
        IntegrationContextImpl resultIntegrationContext = buildIntegrationContext();
        resultIntegrationContext.addInBoundVariables(Map.of("in", "stale-value"));

        IntegrationContextEntity existingEntity = buildIntegrationContextEntity();
        existingEntity.setInBoundVariables(Map.of("in", "requested-value"));

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(existingEntity);
        when(
            entityManager.find(
                ServiceTaskEntity.class,
                IntegrationContextEntity.IdBuilderHelper.from(resultIntegrationContext)
            )
        ).thenReturn(buildServiceTaskEntity());

        handler.handle(buildResultEvent(resultIntegrationContext));

        verify(entityManager).persist(existingEntity);
        assertThat(existingEntity.getInBoundVariables()).containsExactlyEntriesOf(Map.of("in", "requested-value"));
    }

    @Test
    void should_createEntityAndIncrementCounter_when_entityDoesNotExist() {
        IntegrationContextImpl resultIntegrationContext = buildIntegrationContext();
        resultIntegrationContext.addInBoundVariables(Map.of("in", "recovered-value"));
        resultIntegrationContext.addOutBoundVariables(Map.of("out", "result-payload"));

        ServiceTaskEntity serviceTaskEntity = buildServiceTaskEntity();

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(null);
        when(
            entityManager.find(
                ServiceTaskEntity.class,
                IntegrationContextEntity.IdBuilderHelper.from(resultIntegrationContext)
            )
        ).thenReturn(serviceTaskEntity);

        handler.handle(buildResultEvent(resultIntegrationContext));

        ArgumentCaptor<IntegrationContextEntity> captor = ArgumentCaptor.forClass(IntegrationContextEntity.class);
        verify(entityManager).persist(captor.capture());
        assertThat(captor.getValue())
            .returns(INTEGRATION_CONTEXT_ID, IntegrationContextEntity::getId)
            .returns(IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED, IntegrationContextEntity::getStatus)
            .returns(serviceTaskEntity, IntegrationContextEntity::getServiceTask);
        assertThat(captor.getValue().getInBoundVariables()).containsExactlyEntriesOf(Map.of("in", "recovered-value"));
        assertThat(captor.getValue().getOutBoundVariables()).containsExactlyEntriesOf(Map.of("out", "result-payload"));
        assertThat(serviceTaskEntity.getIntegrationContextCounter()).isOne();
    }

    @Test
    void should_persistEntityWithoutServiceTask_when_serviceTaskDoesNotExist() {
        IntegrationContextImpl resultIntegrationContext = buildIntegrationContext();
        resultIntegrationContext.addOutBoundVariables(Map.of("out", "result-payload"));

        IntegrationContextEntity existingEntity = buildIntegrationContextEntity();

        when(entityManager.find(IntegrationContextEntity.class, INTEGRATION_CONTEXT_ID)).thenReturn(existingEntity);
        when(
            entityManager.find(
                ServiceTaskEntity.class,
                IntegrationContextEntity.IdBuilderHelper.from(resultIntegrationContext)
            )
        ).thenReturn(null);

        handler.handle(buildResultEvent(resultIntegrationContext));

        verify(entityManager).persist(existingEntity);
        assertThat(existingEntity.getServiceTask()).isNull();
        assertThat(existingEntity.getOutBoundVariables()).containsExactlyEntriesOf(Map.of("out", "result-payload"));
    }

    @Test
    void should_returnIntegrationResultReceivedEventName_when_getHandledEventIsCalled() {
        assertThat(handler.getHandledEvent()).isEqualTo(INTEGRATION_RESULT_RECEIVED.name());
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

    private CloudIntegrationResultReceivedEventImpl buildResultEvent(IntegrationContextImpl integrationContext) {
        return new CloudIntegrationResultReceivedEventImpl("event-id", System.currentTimeMillis(), integrationContext);
    }
}
