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

package org.activiti.cloud.services.audit.channel;

import org.activiti.cloud.services.audit.converter.TaskJpaJsonConverter;
import org.activiti.cloud.services.audit.events.IgnoredProcessEngineEvent;
import org.activiti.cloud.services.audit.events.ProcessEngineEventEntity;
import org.activiti.cloud.services.audit.events.TaskCancelledEventEntity;
import org.activiti.cloud.services.audit.events.model.Task;
import org.activiti.cloud.services.audit.repository.EventsRepository;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuditConsumerChannelHandlerTest {

    @InjectMocks
    private AuditConsumerChannelHandler handler;

    @Mock
    private EventsRepository eventsRepository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void receiveShouldStoreAllReceivedEventsThatAreNotIgnored() throws Exception {
        //given
        ProcessEngineEventEntity firstEvent = mock(ProcessEngineEventEntity.class);
        ProcessEngineEventEntity secondEvent = mock(ProcessEngineEventEntity.class);
        IgnoredProcessEngineEvent ignoredProcessEngineEvent = new IgnoredProcessEngineEvent();
        ProcessEngineEventEntity[] events = {firstEvent, secondEvent, ignoredProcessEngineEvent};

        //when
        handler.receive(events);

        //then
        verify(eventsRepository).save(firstEvent);
        verify(eventsRepository).save(secondEvent);
        verify(eventsRepository, never()).save(ignoredProcessEngineEvent);
    }

    @Test
    public void testReceiveTaskCancelledEvent() throws Exception {
        //GIVEN

        TaskCancelledEventEntity taskCancelledEventEntity = new TaskCancelledEventEntity();
        Task task = new TaskJpaJsonConverter().convertToEntityAttribute(
                "{\"id\":\"1\",\"createdDate\":1523945813114,\"priority\":50,\"status\":\"CANCELLED\"}");
        FieldUtils.writeField(taskCancelledEventEntity, "task", task, true);
        ProcessEngineEventEntity[] events = new ProcessEngineEventEntity[] {taskCancelledEventEntity};

        //WHEN
        handler.receive(events);

        //THEN
        assertThat(taskCancelledEventEntity.isIgnored()).isFalse();
        assertThat(taskCancelledEventEntity.getTask()).isNotNull();
        assertThat(taskCancelledEventEntity.getTask().getStatus()).isEqualTo("CANCELLED");
        verify(eventsRepository).save(taskCancelledEventEntity);
    }
}