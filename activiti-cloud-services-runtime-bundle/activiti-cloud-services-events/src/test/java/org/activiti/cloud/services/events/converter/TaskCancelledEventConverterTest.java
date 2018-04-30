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

package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.converter.TaskConverter;
import org.activiti.cloud.services.events.TaskCancelledEventImpl;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.task.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.activiti.cloud.services.events.converter.EventConverterContext.getPrefix;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests for {@link TaskCancelledEventConverter}
 */
public class TaskCancelledEventConverterTest {

    @InjectMocks
    private TaskCancelledEventConverter taskCancelledEventConverter;

    @Mock
    private TaskConverter taskConverter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    /**
     * Test that and ActivitiEntityEventImpl with ENTITY_DELETED type and entity of Task
     * will be converted to TaskCancelledEventImpl
     */
    @Test
    public void convertActivitiTaskEntityDeletedEventToTaskCancelledEventImpl() {
        //given
        ActivitiEntityEventImpl activitiEvent = mock(ActivitiEntityEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_DELETED);
        Task internalTask = mock(Task.class);
        given(activitiEvent.getEntity()).willReturn(internalTask);

        org.activiti.cloud.services.api.model.Task externalTask = mock(org.activiti.cloud.services.api.model.Task.class);
        given(taskConverter.from(internalTask)).willReturn(externalTask);

        given(runtimeBundleProperties.getServiceFullName()).willReturn("myApp");

        //when
        ProcessEngineEvent pee = taskCancelledEventConverter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(TaskCancelledEventImpl.class);
        assertThat(((TaskCancelledEventImpl) pee).getTask()).isEqualTo(externalTask);
        assertThat(pee.getServiceFullName()).isEqualTo("myApp");
        assertThat(pee.getEventType()).isEqualTo("TaskCancelledEvent");
    }

    /**
     * Test that TaskCancelledEventConverter handles "Task: ENTITY_DELETED" events type
     */
    @Test
    public void handledTypeShouldReturnTaskCancelled() {
        //when
        ActivitiEntityEventImpl activitiEvent = mock(ActivitiEntityEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_DELETED);
        given(activitiEvent.getEntity()).willReturn(mock(Task.class));
        //then
        assertThat(taskCancelledEventConverter.handledType()).isEqualTo(getPrefix(activitiEvent) + ActivitiEventType.ENTITY_DELETED);
    }
}