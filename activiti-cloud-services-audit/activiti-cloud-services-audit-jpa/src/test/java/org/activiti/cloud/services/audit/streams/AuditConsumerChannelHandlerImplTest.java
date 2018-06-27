/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.audit.streams;

import org.activiti.cloud.services.audit.api.converters.APIEventToEntityConverters;
import org.activiti.cloud.services.audit.api.converters.EventToEntityConverter;
import org.activiti.cloud.services.audit.events.AuditEventEntity;
import org.activiti.cloud.services.audit.repository.EventsRepository;
import org.activiti.runtime.api.event.CloudRuntimeEvent;
import org.activiti.runtime.api.event.ProcessRuntimeEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuditConsumerChannelHandlerImplTest {

    @InjectMocks
    private AuditConsumerChannelHandlerImpl handler;

    @Mock
    private EventsRepository eventsRepository;

    @Mock
    private APIEventToEntityConverters converters;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void receiveEventShouldStoreEntity() throws Exception {
        //given
        CloudRuntimeEvent cloudRuntimeEvent = mock(CloudRuntimeEvent.class);
        when(cloudRuntimeEvent.getEventType()).thenReturn(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED);
        EventToEntityConverter converter = mock(EventToEntityConverter.class);
        when(converters.getConverterByEventTypeName(ProcessRuntimeEvent.ProcessEvents.PROCESS_CREATED.name())).thenReturn(converter);
        AuditEventEntity entity = mock(AuditEventEntity.class);
        when(converter.convertToEntity(cloudRuntimeEvent)).thenReturn(entity);

        CloudRuntimeEvent[] events = {cloudRuntimeEvent};

        //when
        handler.receiveCloudRuntimeEvent(events);

        //then
        verify(eventsRepository).save(entity);
    }

//    @Test
//    public void testReceiveTaskCancelledEvent() throws Exception {
//        //GIVEN
//
//        TaskCancelledEventEntity taskCancelledEventEntity = new TaskCancelledEventEntity();
//        Task task = new TaskJpaJsonConverter().convertToEntityAttribute(
//                "{\"id\":\"1\",\"createdDate\":1523945813114,\"priority\":50,\"status\":\"CANCELLED\"}");
//        FieldUtils.writeField(taskCancelledEventEntity, "task", task, true);
//        AuditEventEntity[] events = new AuditEventEntity[] {taskCancelledEventEntity};
//
//        //WHEN
//        handler.receiveCloudRuntimeEvent(events);
//
//        //THEN
//        assertThat(taskCancelledEventEntity.isIgnored()).isFalse();
//        assertThat(taskCancelledEventEntity.getTask()).isNotNull();
//        assertThat(taskCancelledEventEntity.getTask().getStatus()).isEqualTo("CANCELLED");
//        verify(eventsRepository).save(taskCancelledEventEntity);
//    }
}