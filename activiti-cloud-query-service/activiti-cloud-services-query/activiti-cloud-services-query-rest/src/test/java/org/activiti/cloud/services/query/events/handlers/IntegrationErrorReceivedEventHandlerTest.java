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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.activiti.cloud.api.process.model.CloudIntegrationContext.IntegrationContextStatus;
import org.activiti.cloud.api.process.model.events.CloudIntegrationErrorReceivedEvent;
import org.activiti.cloud.services.query.model.IntegrationContextEntity;
import org.activiti.cloud.services.query.model.ServiceTaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationErrorReceivedEventHandlerTest {

    @Captor
    private ArgumentCaptor<Object> entityManagerPersistCaptor;

    @Mock
    private CloudIntegrationErrorReceivedEvent cloudIntegrationEvent;

    @Mock
    private ServiceTaskEntity serviceTaskEntity;

    @Mock
    private EntityManager entityManager;

    @Test
    void handle() {
        //given
        String errorMessage = "error message";
        int lineNumber = 2137;
        String fileName = "exampleFileName";
        String sourceDeclaringClass = "java.lang.Example";
        String sourceMethodName = "doSomething";
        StackTraceElement originalError = new StackTraceElement(
            sourceDeclaringClass,
            sourceMethodName,
            fileName,
            lineNumber
        );

        IntegrationContextEntity integrationContext = new IntegrationContextEntity();
        when(cloudIntegrationEvent.getErrorMessage()).thenReturn(errorMessage);
        when(cloudIntegrationEvent.getEntity()).thenReturn(integrationContext);
        when(cloudIntegrationEvent.getStackTraceElements()).thenReturn(new ArrayList<>(List.of(originalError)));
        IntegrationErrorReceivedEventHandler handlerInTest = new IntegrationErrorReceivedEventHandler(entityManager);
        when(entityManager.find(IntegrationContextEntity.class, "null:null:null")).thenReturn(integrationContext);
        when(entityManager.find(eq(ServiceTaskEntity.class), anyString())).thenReturn(serviceTaskEntity);

        //when
        handlerInTest.handle(cloudIntegrationEvent);

        //then
        verify(entityManager, times(2)).persist(entityManagerPersistCaptor.capture());
        List<Object> allCapturedValues = entityManagerPersistCaptor.getAllValues();
        assertThat(allCapturedValues).hasSize(2);

        IntegrationContextEntity integrationContextEntity = (IntegrationContextEntity) allCapturedValues.get(0);
        List<StackTraceElement> stackTraceElements = integrationContextEntity.getStackTraceElements();
        assertThat(stackTraceElements).hasSize(2);
        StackTraceElement newStackTraceElement = stackTraceElements.get(0);
        assertThat(newStackTraceElement.getLineNumber()).isEqualTo(lineNumber);
        assertThat(newStackTraceElement.getFileName()).isEqualTo(fileName);
        StackTraceElement sourceStackTraceElement = stackTraceElements.get(1);
        assertThat(sourceStackTraceElement.getLineNumber()).isEqualTo(lineNumber);
        assertThat(sourceStackTraceElement.getFileName()).isEqualTo(fileName);
        assertThat(integrationContextEntity.getStatus()).isEqualTo(IntegrationContextStatus.INTEGRATION_ERROR_RECEIVED);

        ServiceTaskEntity savedServiceTask = (ServiceTaskEntity) allCapturedValues.get(1);
        assertThat(savedServiceTask).isEqualTo(serviceTaskEntity);
    }
}
