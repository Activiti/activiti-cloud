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

import org.activiti.api.runtime.event.impl.BPMNSignalReceivedEventImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNSignalReceivedEventImpl;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CloudSignalReceivedProducerTest {

    @InjectMocks
    private CloudSignalReceivedProducer cloudSignalReceivedProducer;

    @Mock
    private ToCloudProcessRuntimeEventConverter eventConverter;

    @Mock
    private ProcessEngineEventsAggregator eventsAggregator;

    @Test
    public void onEventShouldConvertEventToCloudEventAndAddToAggregator() {
        //given
        BPMNSignalReceivedEventImpl event = new BPMNSignalReceivedEventImpl(new BPMNSignalImpl());
        CloudBPMNSignalReceivedEventImpl cloudEvent = new CloudBPMNSignalReceivedEventImpl();
        given(eventConverter.from(event)).willReturn(cloudEvent);

        //when
        cloudSignalReceivedProducer.onEvent(event);

        //then
        verify(eventsAggregator).add(cloudEvent);
    }
}
