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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Date;
import javax.persistence.EntityManager;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.events.ProcessRuntimeEvent;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCancelledEventImpl;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessCancelledEventHandlerTest {

    @InjectMocks
    private ProcessCancelledEventHandler handler;

    @Mock
    private EntityManager entityManager;

    /**
     * Test that ProcessCancelledEventHandler updates the existing process instance as following:
     * - status to CANCELLED
     * - lastModified to the event time
     */
    @Test
    public void testUpdateExistingProcessInstanceWhenCancelled() {
        //given
        ProcessInstanceEntity processInstanceEntity = mock(ProcessInstanceEntity.class);
        given(entityManager.find(ProcessInstanceEntity.class, "200")).willReturn(processInstanceEntity);

        //when
        handler.handle(createProcessCancelledEvent("200"));

        //then
        verify(entityManager).persist(processInstanceEntity);
        verify(processInstanceEntity).setStatus(ProcessInstance.ProcessInstanceStatus.CANCELLED);
        verify(processInstanceEntity).setLastModified(any(Date.class));
    }

    private CloudRuntimeEvent<?, ?> createProcessCancelledEvent(String processInstanceId) {
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId(processInstanceId);
        return new CloudProcessCancelledEventImpl(processInstance);
    }

    /**
     * Test that ProcessCancelledEventHandler throws QueryException when the related process instance is not found
     */
    @Test
    public void testThrowExceptionWhenProcessInstanceNotFound() {
        //given
        given(entityManager.find(ProcessInstanceEntity.class, "200")).willReturn(null);

        //then
        //when
        assertThatExceptionOfType(QueryException.class)
            .isThrownBy(() -> handler.handle(createProcessCancelledEvent("200")))
            .withMessageContaining("Unable to find process instance with the given id: ");
    }

    /**
     * Test that ProcessCancelledEventHandler is handling ProcessCancelledEvent events
     */
    @Test
    public void getHandledEventShouldReturnProcessCancelledEvent() {
        //when
        String handledEvent = handler.getHandledEvent();

        //then
        assertThat(handledEvent).isEqualTo(ProcessRuntimeEvent.ProcessEvents.PROCESS_CANCELLED.name());
    }
}
