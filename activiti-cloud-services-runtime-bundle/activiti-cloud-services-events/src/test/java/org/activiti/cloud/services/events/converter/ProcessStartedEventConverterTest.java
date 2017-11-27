/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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
import org.activiti.cloud.services.events.ProcessStartedEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.ActivitiProcessStartedEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessStartedEventConverterTest {

    @InjectMocks
    private ProcessStartedEventConverter converter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalProcessStartedEventToExternalEvent() throws Exception {
        //given
        ActivitiProcessStartedEvent activitiEvent = mock(ActivitiProcessStartedEvent.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.PROCESS_STARTED);
        given(activitiEvent.getExecutionId()).willReturn("1");
        given(activitiEvent.getProcessInstanceId()).willReturn("10");
        given(activitiEvent.getProcessDefinitionId()).willReturn("myProcessDef");
        given(activitiEvent.getNestedProcessDefinitionId()).willReturn("myParentProcessDef");
        given(activitiEvent.getNestedProcessInstanceId()).willReturn("2");

        given(runtimeBundleProperties.getName()).willReturn("myApp");

        //when
        ProcessEngineEvent pee = converter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(ProcessStartedEvent.class);
        ProcessStartedEvent processStartedEvent = (ProcessStartedEvent) pee;

        assertThat(processStartedEvent.getExecutionId()).isEqualTo("1");
        assertThat(processStartedEvent.getProcessInstanceId()).isEqualTo("10");
        assertThat(processStartedEvent.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processStartedEvent.getNestedProcessDefinitionId()).isEqualTo("myParentProcessDef");
        assertThat(processStartedEvent.getNestedProcessInstanceId()).isEqualTo("2");
        assertThat(processStartedEvent.getApplicationName()).isEqualTo("myApp");
    }

    @Test
    public void handledTypeShouldReturnProcessStarted() throws Exception {
        //when
        ActivitiEventType activitiEventType = converter.handledType();

        //then
        assertThat(activitiEventType).isEqualTo(ActivitiEventType.PROCESS_STARTED);
    }
}