/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.events.listeners;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.events.BPMNMessageReceivedEvent;
import org.activiti.api.process.model.events.BPMNMessageSentEvent;
import org.activiti.api.process.model.events.BPMNMessageWaitingEvent;
import org.activiti.api.runtime.event.impl.BPMNMessageReceivedEventImpl;
import org.activiti.api.runtime.event.impl.BPMNMessageSentEventImpl;
import org.activiti.api.runtime.event.impl.BPMNMessageWaitingEventImpl;
import org.activiti.api.runtime.model.impl.BPMNMessageImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageSentEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNMessageWaitingEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageReceivedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageSentEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudBPMNMessageWaitingEventImpl;
import org.activiti.cloud.services.events.converter.ToCloudProcessRuntimeEventConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CloudMessageProducerTest {

    @InjectMocks
    private CloudMessageSentProducer cloudMessageSentProducer;
    
    @InjectMocks
    private CloudMessageWaitingProducer cloudMessageWaitingProducer;

    @InjectMocks
    private CloudMessageReceivedProducer cloudMessageReceivedProducer;
    
    @Mock
    private ToCloudProcessRuntimeEventConverter eventConverter;
    
    @Mock
    private ProcessEngineEventsAggregator eventsAggregator;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertMessageSentEventToCloudEventAndAddToAggregator() {
        //given
        BPMNMessageSentEvent eventFired = new BPMNMessageSentEventImpl(new BPMNMessageImpl());
        CloudBPMNMessageSentEvent cloudEventFired = new CloudBPMNMessageSentEventImpl();
        
        given(eventConverter.from(eventFired)).willReturn(cloudEventFired);
        
        //when
        cloudMessageSentProducer.onEvent(eventFired);

        //then
        verify(eventsAggregator).add(cloudEventFired);
    }
    
    @Test
    public void shouldConvertMessageWaitingEventToCloudEventAndAddToAggregator() {
        //given
        BPMNMessageWaitingEvent eventFired = new BPMNMessageWaitingEventImpl(new BPMNMessageImpl());
        CloudBPMNMessageWaitingEvent cloudEventFired = new CloudBPMNMessageWaitingEventImpl();
        
        given(eventConverter.from(eventFired)).willReturn(cloudEventFired);
        
        //when
        cloudMessageWaitingProducer.onEvent(eventFired);

        //then
        verify(eventsAggregator).add(cloudEventFired);
    }
    
    @Test
    public void shouldConvertMessageReceivedEventToCloudEventAndAddToAggregator() {
        //given
        BPMNMessageReceivedEvent eventFired = new BPMNMessageReceivedEventImpl(new BPMNMessageImpl());
        CloudBPMNMessageReceivedEvent cloudEventFired = new CloudBPMNMessageReceivedEventImpl();
        
        given(eventConverter.from(eventFired)).willReturn(cloudEventFired);
        
        //when
        cloudMessageReceivedProducer.onEvent(eventFired);

        //then
        verify(eventsAggregator).add(cloudEventFired);
    }    
    
}