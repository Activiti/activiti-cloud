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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.payloads.TimerPayload;
import org.activiti.api.runtime.event.impl.BPMNTimerCancelledEventImpl;
import org.activiti.api.runtime.event.impl.BPMNTimerExecutedEventImpl;
import org.activiti.api.runtime.event.impl.BPMNTimerFailedEventImpl;
import org.activiti.api.runtime.event.impl.BPMNTimerFiredEventImpl;
import org.activiti.api.runtime.event.impl.BPMNTimerRetriesDecrementedEventImpl;
import org.activiti.api.runtime.event.impl.BPMNTimerScheduledEventImpl;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerCancelledEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerExecutedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFailedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFiredEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerRetriesDecrementedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerScheduledEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ToCloudProcessRuntimeTimerEventsConverterTest {

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertBPMNTimerFiredEventToCloudBPMNTimerFiredEvent() {
        //given
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessInstanceId("procInstId");
        timer.setProcessDefinitionId("procDefId");
        TimerPayload timerPayload = new TimerPayload();

        timer.setTimerPayload(timerPayload);
        BPMNTimerFiredEventImpl timerFiredEvent = new BPMNTimerFiredEventImpl(timer);

        //when
        CloudBPMNTimerFiredEvent cloudEvent = converter.from(timerFiredEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(timer);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
    
    @Test
    public void shouldConvertBPMNTimerScheduledEventToCloudBPMNTimerScheduledEvent() {
        //given
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessInstanceId("procInstId");
        timer.setProcessDefinitionId("procDefId");
        TimerPayload timerPayload = new TimerPayload();

        timer.setTimerPayload(timerPayload);
        BPMNTimerScheduledEventImpl timerFiredEvent = new BPMNTimerScheduledEventImpl(timer);

        //when
        CloudBPMNTimerScheduledEvent cloudEvent = converter.from(timerFiredEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(timer);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
    
    @Test
    public void shouldConvertBPMNTimerCancelledEventToCloudBPMNTimerCancelledEvent() {
        //given
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessInstanceId("procInstId");
        timer.setProcessDefinitionId("procDefId");
        TimerPayload timerPayload = new TimerPayload();

        timer.setTimerPayload(timerPayload);
        BPMNTimerCancelledEventImpl timerFiredEvent = new BPMNTimerCancelledEventImpl(timer);

        //when
        CloudBPMNTimerCancelledEvent cloudEvent = converter.from(timerFiredEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(timer);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
    
    @Test
    public void shouldConvertBPMNTimerExecutedEventToCloudBPMNTimerExecutedEvent() {
        //given
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessInstanceId("procInstId");
        timer.setProcessDefinitionId("procDefId");
        TimerPayload timerPayload = new TimerPayload();

        timer.setTimerPayload(timerPayload);
        BPMNTimerExecutedEventImpl timerFiredEvent = new BPMNTimerExecutedEventImpl(timer);

        //when
        CloudBPMNTimerExecutedEvent cloudEvent = converter.from(timerFiredEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(timer);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
    
    @Test
    public void shouldConvertBPMNTimerFailedEventToCloudBPMNTimerFailedEvent() {
        //given
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessInstanceId("procInstId");
        timer.setProcessDefinitionId("procDefId");
        TimerPayload timerPayload = new TimerPayload();

        timer.setTimerPayload(timerPayload);
        BPMNTimerFailedEventImpl timerFiredEvent = new BPMNTimerFailedEventImpl(timer);

        //when
        CloudBPMNTimerFailedEvent cloudEvent = converter.from(timerFiredEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(timer);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
    
    @Test
    public void shouldConvertBPMNTimerRetriesDecrementedEventToCloudBPMNTimerRetriesDecrementedEvent() {
        //given
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessInstanceId("procInstId");
        timer.setProcessDefinitionId("procDefId");
        TimerPayload timerPayload = new TimerPayload();

        timer.setTimerPayload(timerPayload);
        BPMNTimerRetriesDecrementedEventImpl timerFiredEvent = new BPMNTimerRetriesDecrementedEventImpl(timer);

        //when
        CloudBPMNTimerRetriesDecrementedEvent cloudEvent = converter.from(timerFiredEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(timer);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
}