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

import org.activiti.api.process.model.events.BPMNErrorReceivedEvent;
import org.activiti.api.runtime.event.impl.BPMNErrorReceivedEventImpl;
import org.activiti.api.runtime.model.impl.BPMNErrorImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNErrorReceivedEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNErrorReceivedEventImpl;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CloudErrorProducerTest {

    @InjectMocks
    private CloudErrorReceivedProducer cloudErrorReceivedProducer;

    @Mock
    private ToCloudProcessRuntimeEventConverter eventConverter;

    @Mock
    private ProcessEngineEventsAggregator eventsAggregator;

    @Test
    public void shouldConvertErrorReceivedEventToCloudEventAndAddToAggregator() {
        BPMNErrorReceivedEvent eventFired = new BPMNErrorReceivedEventImpl(new BPMNErrorImpl());
        CloudBPMNErrorReceivedEvent cloudEventFired = new CloudBPMNErrorReceivedEventImpl();

        given(eventConverter.from(eventFired)).willReturn(cloudEventFired);

        cloudErrorReceivedProducer.onEvent(eventFired);

        verify(eventsAggregator).add(cloudEventFired);
    }
}
