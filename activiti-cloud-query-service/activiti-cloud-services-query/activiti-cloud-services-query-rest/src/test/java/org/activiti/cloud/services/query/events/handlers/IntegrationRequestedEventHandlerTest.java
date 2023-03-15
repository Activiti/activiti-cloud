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
package org.activiti.cloud.services.query.events.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Date;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.IntegrationContext;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
import org.activiti.cloud.api.process.model.CloudBPMNActivity;
import org.activiti.cloud.api.process.model.events.CloudIntegrationRequestedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudIntegrationRequestedEventImpl;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationRequestedEventHandlerTest {

    @InjectMocks
    private IntegrationRequestedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    void testIntegrationRequestedEventShouldUpdateServiceTaskStatusToStarted() {
        //given
        CloudIntegrationRequestedEvent event = createIntegrationRequestedEvent();
        String entityId = IntegrationContextEntity.IdBuilderHelper.from(event.getEntity());

        ServiceTaskEntity serviceTaskEntity = mock(ServiceTaskEntity.class);
        IntegrationContextEntity integrationContextEntity = mock(IntegrationContextEntity.class);

        given(entityManager.find(IntegrationContextEntity.class, entityId)).willReturn(integrationContextEntity);
        given(entityManager.find(ServiceTaskEntity.class, entityId)).willReturn(serviceTaskEntity);

        //when
        handler.handle(event);

        //then
        verify(serviceTaskEntity).setStatus(eq(CloudBPMNActivity.BPMNActivityStatus.STARTED));
        verify(serviceTaskEntity).setStartedDate(any(Date.class));
        verify(serviceTaskEntity).setCompletedDate(eq(null));

        verify(entityManager).persist(integrationContextEntity);
    }

    private CloudIntegrationRequestedEvent createIntegrationRequestedEvent() {
        IntegrationContext integrationContext = new IntegrationContextImpl() {
            {
                setClientId("clientId");
                setExecutionId("executionId");
                setProcessInstanceId("processInstanceId");
            }
        };

        return new CloudIntegrationRequestedEventImpl(integrationContext);
    }
}
