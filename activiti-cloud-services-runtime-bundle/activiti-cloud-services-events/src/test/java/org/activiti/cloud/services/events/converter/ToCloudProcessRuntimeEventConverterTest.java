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

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.runtime.api.event.CloudProcessStartedEvent;
import org.activiti.runtime.api.event.impl.ProcessStartedEventImpl;
import org.activiti.runtime.api.model.impl.FluentProcessInstanceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

public class ToCloudProcessRuntimeEventConverterTest {

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalProcessStartedEventToExternalEvent() {
        //given
        FluentProcessInstanceImpl processInstance = new FluentProcessInstanceImpl(null, null);
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("myProcessDef");

        ProcessStartedEventImpl event = new ProcessStartedEventImpl(processInstance);
        event.setNestedProcessDefinitionId("myParentProcessDef");
        event.setNestedProcessInstanceId("2");

        given(runtimeBundleProperties.getServiceFullName()).willReturn("myApp");


        //when
        CloudProcessStartedEvent pee = converter.from(event);

        //then
        assertThat(pee).isInstanceOf(org.activiti.cloud.services.events.ProcessStartedEvent.class);
        org.activiti.cloud.services.events.ProcessStartedEvent processStartedEvent = (org.activiti.cloud.services.events.ProcessStartedEvent) pee;

        assertThat(processStartedEvent.getExecutionId()).isEqualTo("1");
        assertThat(processStartedEvent.getProcessInstanceId()).isEqualTo("10");
        assertThat(processStartedEvent.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processStartedEvent.getNestedProcessDefinitionId()).isEqualTo("myParentProcessDef");
        assertThat(processStartedEvent.getNestedProcessInstanceId()).isEqualTo("2");
        assertThat(processStartedEvent.getServiceFullName()).isEqualTo("myApp");
    }

}