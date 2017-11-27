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
import org.activiti.cloud.services.events.VariableCreatedEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiVariableEventImpl;
import org.activiti.engine.impl.variable.StringType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class VariableCreatedEventConverterTest {

    @InjectMocks
    private VariableCreatedEventConverter converter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void internalVariableEventToExternalConvertion() throws Exception {
        //given
        given(runtimeBundleProperties.getName()).willReturn("myApp");

        ActivitiVariableEventImpl activitiEvent = mock(ActivitiVariableEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.VARIABLE_CREATED);
        given(activitiEvent.getExecutionId()).willReturn("1");
        given(activitiEvent.getProcessInstanceId()).willReturn("1");
        given(activitiEvent.getProcessDefinitionId()).willReturn("myProcessDef");
        given(activitiEvent.getTaskId()).willReturn("1");
        given(activitiEvent.getVariableName()).willReturn("myVar");
        given(activitiEvent.getVariableType()).willReturn(new StringType(255));
        given(activitiEvent.getVariableValue()).willReturn(null);

        ProcessEngineEvent pee = converter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(VariableCreatedEvent.class);
        assertThat(pee.getExecutionId()).isEqualTo("1");
        assertThat(pee.getProcessInstanceId()).isEqualTo("1");
        assertThat(pee.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(pee.getApplicationName()).isEqualTo("myApp");
        assertThat(((VariableCreatedEvent) pee).getTaskId()).isEqualTo("1");
        assertThat(((VariableCreatedEvent) pee).getVariableName()).isEqualTo("myVar");
        assertThat(((VariableCreatedEvent) pee).getVariableType()).isEqualTo("string");
        assertThat(((VariableCreatedEvent) pee).getVariableValue()).isEqualTo("");
    }

    @Test
    public void handledTypeShouldReturnVariableCreated() throws Exception {
        //when
        ActivitiEventType handledType = converter.handledType();

        //then
        assertThat(handledType).isEqualTo(ActivitiEventType.VARIABLE_CREATED);
    }

}