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
import org.activiti.cloud.services.events.TaskSuspendedEvent;
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

public class TaskSuspendedEventConverterTest {

    @InjectMocks
    private TaskSuspendedEventConverter taskSuspendedEventConverter;

    @Mock
    private TaskConverter taskConverter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalTaskSuspendedEventToExternalEvent() throws Exception {
        //given
        ActivitiEntityEventImpl activitiEvent = mock(ActivitiEntityEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_SUSPENDED);
        given(activitiEvent.getExecutionId()).willReturn("1");
        given(activitiEvent.getProcessInstanceId()).willReturn("1");
        given(activitiEvent.getProcessDefinitionId()).willReturn("myProcessDef");

        Task internalTask = mock(Task.class);
        given(activitiEvent.getEntity()).willReturn(internalTask);

        org.activiti.cloud.services.api.model.Task externalTask = mock(org.activiti.cloud.services.api.model.Task.class);
        given(taskConverter.from(internalTask)).willReturn(externalTask);

        given(runtimeBundleProperties.getFullyQualifiedServiceName()).willReturn("myApp");

        //when
        ProcessEngineEvent pee = taskSuspendedEventConverter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(TaskSuspendedEvent.class);
        assertThat(pee.getExecutionId()).isEqualTo("1");
        assertThat(pee.getProcessInstanceId()).isEqualTo("1");
        assertThat(pee.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(pee.getFullyQualifiedServiceName()).isEqualTo("myApp");
        assertThat(((TaskSuspendedEvent) pee).getTask()).isEqualTo(externalTask);
    }

    @Test
    public void handledTypeShouldReturnTaskCompleted() throws Exception {
        //when
        String activitiEventType = taskSuspendedEventConverter.handledType();
        ActivitiEntityEventImpl activitiEvent = mock(ActivitiEntityEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_SUSPENDED);
        Task internalTask = mock(Task.class);
        given(activitiEvent.getEntity()).willReturn(internalTask);
        //then
        assertThat(activitiEventType).isEqualTo(getPrefix(activitiEvent) + ActivitiEventType.ENTITY_SUSPENDED);
    }
}