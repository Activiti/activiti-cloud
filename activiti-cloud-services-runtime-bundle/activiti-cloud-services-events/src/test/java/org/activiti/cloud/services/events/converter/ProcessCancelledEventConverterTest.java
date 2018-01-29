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
import org.activiti.cloud.services.events.ProcessCancelledEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiProcessCancelledEventImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessCancelledEventConverterTest {

    @InjectMocks
    private ProcessCancelledEventConverter converter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalProcessCancelledEventToExternalEvent() throws Exception {
        //given
        ActivitiProcessCancelledEventImpl activitiEvent = mock(ActivitiProcessCancelledEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.PROCESS_CANCELLED);
        given(activitiEvent.getExecutionId()).willReturn("1");
        given(activitiEvent.getProcessInstanceId()).willReturn("1");
        given(activitiEvent.getProcessDefinitionId()).willReturn("myProcessDef");
        given(activitiEvent.getCause()).willReturn("cause of the cancellation");

        given(runtimeBundleProperties.getName()).willReturn("myApp");

        //when
        ProcessEngineEvent pee = converter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(ProcessCancelledEvent.class);
        assertThat(pee.getExecutionId()).isEqualTo("1");
        assertThat(pee.getProcessInstanceId()).isEqualTo("1");
        assertThat(pee.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(pee.getApplicationName()).isEqualTo("myApp");
        assertThat(((ProcessCancelledEvent) pee).getCause()).isEqualTo("cause of the cancellation");
    }

    @Test
    public void handledTypeShouldReturnProcessCancelled() throws Exception {
        //when
        ActivitiEventType activitiEventType = converter.handledType();

        //then
        assertThat(activitiEventType).isEqualTo(ActivitiEventType.PROCESS_CANCELLED);
    }

}