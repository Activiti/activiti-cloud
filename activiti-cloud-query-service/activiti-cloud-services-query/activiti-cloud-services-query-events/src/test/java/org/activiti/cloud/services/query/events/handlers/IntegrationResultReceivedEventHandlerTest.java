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
public class IntegrationResultReceivedEventHandlerTest extends IntegrationEventsHelper {

    @InjectMocks
    private IntegrationResultReceivedEventHandler resultHandler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldUpdateStatusesWhenIntegrationResultReceived() {
        // given
        String id = UUID.randomUUID().toString();
        IntegrationContextEntity existingIntegrationContextEntity = createIntegrationContextEntity(id);

        existingIntegrationContextEntity.setServiceTask(createServiceTaskEntity(id));

        when(entityManager.find(IntegrationContextEntity.class, id)).thenReturn(existingIntegrationContextEntity);

        // when
        CloudIntegrationResultReceivedEvent resultEvent = buildIntegrationResultReceivedEvent(id);
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

    @Test
    public void handleShouldUpdateStatusesForLegacyIdsWhenIntegrationResultReceived() {
        // given
        String newUuid = UUID.randomUUID().toString();
        String legacyCompositeKeyId = getLegacyId();

        IntegrationContextEntity existingIntegrationContextEntity = createIntegrationContextEntity(
            legacy_composite_key_id
        );

        existingIntegrationContextEntity.setServiceTask(createServiceTaskEntity(legacy_composite_key_id));

        // First attempt with UUID returns null, second attempt with legacy composite key returns the entity
        when(entityManager.find(IntegrationContextEntity.class, new_uuid_id)).thenReturn(null);
        when(entityManager.find(IntegrationContextEntity.class, legacy_composite_key_id))
            .thenReturn(existingIntegrationContextEntity);

        // when
        CloudIntegrationResultReceivedEvent resultEvent = buildIntegrationResultReceivedEvent(new_uuid_id);
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
}
