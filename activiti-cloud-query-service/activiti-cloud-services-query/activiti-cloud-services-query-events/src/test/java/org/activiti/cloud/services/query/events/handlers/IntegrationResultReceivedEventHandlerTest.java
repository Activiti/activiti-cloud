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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.activiti.api.runtime.model.impl.ExternalizedDataConfigImpl;
import org.activiti.api.runtime.model.impl.IntegrationContextImpl;
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
    private IntegrationResultReceivedEventHandler handler;

    @Mock
    private EntityManager entityManager;

    @Test
    public void handleShouldMoveExternalizedDataConfigFromContextWhenEntityIsMissing() {
        IntegrationContextImpl integrationContext = new IntegrationContextImpl();
        integrationContext.setProcessInstanceId("procInst");
        integrationContext.setClientId("client");
        integrationContext.setExecutionId("exec");
        integrationContext.setExternalizedDataConfig(
            new ExternalizedDataConfigImpl("temp-storage", "https://temp-storage/blobs")
        );

        CloudIntegrationResultReceivedEvent event = new CloudIntegrationResultReceivedEventImpl(integrationContext);
        String serviceTaskId = IntegrationContextEntity.IdBuilderHelper.from(integrationContext);

        when(entityManager.find(eq(IntegrationContextEntity.class), eq(integrationContext.getId()))).thenReturn(null);
        when(entityManager.find(eq(ServiceTaskEntity.class), eq(serviceTaskId))).thenReturn(null);

        handler.handle(event);

        ArgumentCaptor<IntegrationContextEntity> captor = ArgumentCaptor.forClass(IntegrationContextEntity.class);
        verify(entityManager).persist(captor.capture());

        IntegrationContextEntity entity = captor.getValue();
        assertThat(entity.getExternalizedDataConfig()).isNotNull();
        assertThat(entity.getExternalizedDataConfig().getProviderType()).isEqualTo("temp-storage");
        assertThat(entity.getExternalizedDataConfig().getUrl()).isEqualTo("https://temp-storage/blobs");
    }
}
