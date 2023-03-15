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
package org.activiti.cloud.services.events.listeners;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.activiti.api.runtime.event.impl.BPMNTimerFiredEventImpl;
import org.activiti.api.runtime.event.impl.BPMNTimerScheduledEventImpl;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerFiredEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNTimerScheduledEventImpl;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CloudTimerProducerTest {

    @InjectMocks
    private CloudTimerFiredProducer cloudTimerFiredProducer;

    @InjectMocks
    private CloudTimerScheduledProducer cloudTimerScheduledProducer;

    @Mock
    private ToCloudProcessRuntimeEventConverter eventConverter;

    @Mock
    private ProcessEngineEventsAggregator eventsAggregator;

    @Test
    public void onEventShouldConvertEventToCloudEventAndAddToAggregator() {
        //given
        BPMNTimerFiredEventImpl eventFired = new BPMNTimerFiredEventImpl(new BPMNTimerImpl());
        CloudBPMNTimerFiredEventImpl cloudEventFired = new CloudBPMNTimerFiredEventImpl();
        given(eventConverter.from(eventFired)).willReturn(cloudEventFired);
        //when
        cloudTimerFiredProducer.onEvent(eventFired);

        //then
        verify(eventsAggregator).add(cloudEventFired);

        BPMNTimerScheduledEventImpl eventScheduled = new BPMNTimerScheduledEventImpl(new BPMNTimerImpl());
        CloudBPMNTimerScheduledEventImpl cloudEventScheduled = new CloudBPMNTimerScheduledEventImpl();
        given(eventConverter.from(eventScheduled)).willReturn(cloudEventScheduled);

        //when
        cloudTimerScheduledProducer.onEvent(eventScheduled);

        //then
        verify(eventsAggregator).add(cloudEventScheduled);
    }
}
