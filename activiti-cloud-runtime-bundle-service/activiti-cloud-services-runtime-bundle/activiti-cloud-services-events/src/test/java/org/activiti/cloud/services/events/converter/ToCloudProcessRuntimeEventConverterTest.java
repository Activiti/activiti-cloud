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
import static org.mockito.Mockito.verify;

import org.activiti.api.runtime.event.impl.BPMNSignalReceivedEventImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessCompletedEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.runtime.api.event.impl.ProcessCompletedImpl;
import org.activiti.runtime.api.event.impl.ProcessStartedEventImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToCloudProcessRuntimeEventConverterTest {

    private static final String SERVICE_USER = "service_user";

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    private static final String USERNAME = "user1";

    @Test
    void fromShouldConvertInternalProcessStartedEventToExternalEvent() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("myProcessDef");

        ProcessStartedEventImpl event = new ProcessStartedEventImpl(processInstance);
        event.setNestedProcessDefinitionId("myParentProcessDef");
        event.setNestedProcessInstanceId("2");

        //when
        CloudProcessStartedEvent processStarted = this.converter.from(event);

        //then
        assertThat(processStarted).isInstanceOf(CloudProcessStartedEvent.class);

        assertThat(processStarted.getEntity().getId()).isEqualTo("10");
        assertThat(processStarted.getEntity().getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processStarted.getNestedProcessDefinitionId()).isEqualTo("myParentProcessDef");
        assertThat(processStarted.getNestedProcessInstanceId()).isEqualTo("2");
        assertThat(processStarted.getActor()).isEqualTo(SERVICE_USER);

        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void shouldConvertBPMNSignalReceivedEventToCloudBPMNSignalReceivedEvent() {
        //given
        BPMNSignalImpl signal = new BPMNSignalImpl();
        signal.setProcessDefinitionId("procDefId");
        signal.setProcessInstanceId("procInstId");
        BPMNSignalReceivedEventImpl signalReceivedEvent = new BPMNSignalReceivedEventImpl(signal);

        //when
        CloudBPMNSignalReceivedEvent cloudEvent = this.converter.from(signalReceivedEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(signal);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }

    @Test
    void should_convertInternalProcessCompletedEvent_when_convertToExternalEvent() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("myProcessDef");
        processInstance.setInitiator(USERNAME);

        ProcessCompletedImpl event = new ProcessCompletedImpl(processInstance);

        //when
        CloudProcessCompletedEvent processCompleted = converter.from(event);

        //then
        assertThat(processCompleted).isInstanceOf(CloudProcessCompletedEvent.class);

        assertThat(processCompleted.getEntity().getId()).isEqualTo("10");
        assertThat(processCompleted.getEntity().getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processCompleted.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processCompleted.getProcessInstanceId()).isEqualTo("10");
        assertThat(processCompleted.getActor()).isEqualTo(SERVICE_USER);

        verify(this.runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(any(CloudRuntimeEventImpl.class));
    }
}
