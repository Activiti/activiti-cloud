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
package org.activiti.cloud.services.events.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.runtime.event.impl.BPMNSignalReceivedEventImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.ProcessDefinitionImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.runtime.api.event.impl.ProcessStartedEventImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ToCloudProcessRuntimeEventConverterTest {

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalProcessStartedEventToExternalEvent() {

        final ProcessDefinitionEntityImpl processDefinition = new ProcessDefinitionEntityImpl();
        processDefinition.setName("myProcessDefName");

        final ToCloudProcessRuntimeEventConverter spy = Mockito.spy(converter);

        doReturn(processDefinition).when(spy).getProcessDefinition("myProcessDef");

        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("myProcessDef");

        ProcessStartedEventImpl event = new ProcessStartedEventImpl(processInstance);
        event.setNestedProcessDefinitionId("myParentProcessDef");
        event.setNestedProcessInstanceId("2");

        //when
        CloudProcessStartedEvent processStarted = spy.from(event);

        //then
        assertThat(processStarted).isInstanceOf(CloudProcessStartedEvent.class);

        assertThat(processStarted.getEntity().getId()).isEqualTo("10");
        assertThat(processStarted.getEntity().getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processStarted.getEntity().getProcessDefinitionName()).isEqualTo("myProcessDefName");
        assertThat(processStarted.getNestedProcessDefinitionId()).isEqualTo("myParentProcessDef");
        assertThat(processStarted.getNestedProcessInstanceId()).isEqualTo("2");

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    public void shouldConvertBPMNSignalReceivedEventToCloudBPMNSignalReceivedEvent() {
        //given
        BPMNSignalImpl signal = new BPMNSignalImpl();
        signal.setProcessDefinitionId("procDefId");
        signal.setProcessInstanceId("procInstId");
        BPMNSignalReceivedEventImpl signalReceivedEvent = new BPMNSignalReceivedEventImpl(signal);

        //when
        CloudBPMNSignalReceivedEvent cloudEvent = converter.from(signalReceivedEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(signal);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }
}
