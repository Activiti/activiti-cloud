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
import java.util.UUID;
import org.activiti.cloud.api.process.model.CloudBPMNActivity.BPMNActivityStatus;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IntegrationRequestedEventHandlerTest extends IntegrationEventsHelper {

    @InjectMocks
    private IntegrationRequestedEventHandler requestedHandler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldCreateNewIntegrationContextAndServiceTaskWhenIntegrationRequested() {
        // given
        String id = UUID.randomUUID().toString();

        when(entityManager.find(IntegrationContextEntity.class, id)).thenReturn(null);

        // when
        CloudIntegrationRequestedEvent requestedEvent = buildIntegrationRequestedEvent(id);
        requestedHandler.handle(requestedEvent);

        // then
        ArgumentCaptor<IntegrationContextEntity> integrationContextCaptor = ArgumentCaptor.forClass(
            IntegrationContextEntity.class
        );
        ArgumentCaptor<ServiceTaskEntity> serviceTaskCaptor = ArgumentCaptor.forClass(ServiceTaskEntity.class);

        verify(entityManager, times(2)).persist(any());
        verify(entityManager).persist(serviceTaskCaptor.capture());
        verify(entityManager).persist(integrationContextCaptor.capture());

        IntegrationContextEntity savedIntegrationContext = integrationContextCaptor.getValue();
        ServiceTaskEntity savedServiceTask = serviceTaskCaptor.getValue();

        assertThat(savedIntegrationContext.getId()).isEqualTo(id);
        assertThat(savedIntegrationContext.getProcessInstanceId()).isEqualTo(PROCESS_INSTANCE_ID);
        assertThat(savedIntegrationContext.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(savedIntegrationContext.getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(savedIntegrationContext.getStatus()).isEqualTo(IntegrationContextStatus.INTEGRATION_REQUESTED);

        assertThat(savedServiceTask.getId()).isEqualTo(id);
        assertThat(savedServiceTask.getElementId()).isEqualTo(CLIENT_ID);
        assertThat(savedServiceTask.getProcessInstanceId()).isEqualTo(PROCESS_INSTANCE_ID);
        assertThat(savedServiceTask.getExecutionId()).isEqualTo(EXECUTION_ID);
        assertThat(savedServiceTask.getStatus()).isEqualTo(BPMNActivityStatus.STARTED);
    }

    private CloudIntegrationRequestedEvent buildIntegrationRequestedEvent(String integrationContextId) {
        CloudIntegrationRequestedEventImpl event = new CloudIntegrationRequestedEventImpl(
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
}
