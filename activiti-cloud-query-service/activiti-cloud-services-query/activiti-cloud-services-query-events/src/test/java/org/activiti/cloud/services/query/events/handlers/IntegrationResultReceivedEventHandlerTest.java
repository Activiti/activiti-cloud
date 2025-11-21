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
package org.activiti.cloud.services.query.events.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Date;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationResultReceivedEvent;
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
public class IntegrationResultReceivedEventHandlerTest {

    @InjectMocks
    private IntegrationResultReceivedEventHandler resultHandler;

    @Mock
    private EntityManager entityManager;

    private static final String uuid = "f89b5dc9-ab00-4c89-bbf1-65fc013371fd";

    @Test
    public void handleShouldUpdateStatusFromRequestedToResultReceived() {
        // given
        IntegrationContextEntity existingIntegrationContextEntity = createIntegrationContextEntity(
            uuid,
            IntegrationContextStatus.INTEGRATION_REQUESTED
        );

        existingIntegrationContextEntity.setServiceTask(createServiceTaskEntity(uuid, BPMNActivityStatus.STARTED));

        when(entityManager.find(IntegrationContextEntity.class, uuid)).thenReturn(existingIntegrationContextEntity);

        // when
        CloudIntegrationResultReceivedEvent resultEvent = buildIntegrationResultReceivedEvent(uuid);
        resultHandler.handle(resultEvent);

        // then
        ArgumentCaptor<IntegrationContextEntity> integrationContextCaptor = ArgumentCaptor.forClass(
            IntegrationContextEntity.class
        );
        ArgumentCaptor<ServiceTaskEntity> serviceTaskCaptor = ArgumentCaptor.forClass(ServiceTaskEntity.class);

        verify(entityManager, times(2)).persist(any());
        verify(entityManager).persist(integrationContextCaptor.capture());
        verify(entityManager).persist(serviceTaskCaptor.capture());

        IntegrationContextEntity updatedIntegrationContext = integrationContextCaptor.getValue();
        ServiceTaskEntity updatedServiceTask = serviceTaskCaptor.getValue();

        assertThat(updatedIntegrationContext.getStatus())
            .isEqualTo(IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED);
        assertThat(updatedIntegrationContext.getResultDate()).isNotNull();

        assertThat(updatedServiceTask.getStatus()).isEqualTo(BPMNActivityStatus.COMPLETED);
        assertThat(updatedServiceTask.getCompletedDate()).isNotNull();
    }

    private CloudIntegrationResultReceivedEvent buildIntegrationResultReceivedEvent(String integrationContextId) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(integrationContextId);
        integrationContext.setProcessInstanceId("processInstanceId");
        integrationContext.setClientId("clientId");
        integrationContext.setExecutionId("executionId");

        CloudIntegrationResultReceivedEventImpl event = new CloudIntegrationResultReceivedEventImpl(
            "event-id",
            new Date().getTime(),
            createIntegrationContext(integrationContextId)
        );
        event.setServiceName("serviceName");
        event.setServiceFullName("serviceFullName");
        event.setServiceVersion("serviceVersion");
        event.setAppName("appName");
        event.setAppVersion("appVersion");

        return event;
    }

    private IntegrationContextImpl createIntegrationContext(String integrationContextId) {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setId(integrationContextId);
        integrationContext.setProcessInstanceId("processInstanceId");
        integrationContext.setClientId("clientId");
        integrationContext.setExecutionId("executionId");
        return integrationContext;
    }

    private IntegrationContextEntity createIntegrationContextEntity(
        String id,
        IntegrationContextStatus integrationContextStatus
    ) {
        IntegrationContextEntity existingEntity = new IntegrationContextEntity(
            "serviceName",
            "serviceFullName",
            "serviceVersion",
            "appName",
            "appVersion"
        );
        existingEntity.setId(id);
        existingEntity.setProcessInstanceId("processInstanceId");
        existingEntity.setClientId("clientId");
        existingEntity.setExecutionId("executionId");
        existingEntity.setStatus(integrationContextStatus);
        existingEntity.setRequestDate(new Date());

        return existingEntity;
    }

    private ServiceTaskEntity createServiceTaskEntity(String id, BPMNActivityStatus bpmnActivityStatus) {
        ServiceTaskEntity serviceTaskEntity = new ServiceTaskEntity(
            "serviceName",
            "serviceFullName",
            "serviceVersion",
            "appName",
            "appVersion"
        );
        serviceTaskEntity.setId(id);
        serviceTaskEntity.setStatus(bpmnActivityStatus);
        serviceTaskEntity.setStartedDate(new Date());

        return serviceTaskEntity;
    }
}
