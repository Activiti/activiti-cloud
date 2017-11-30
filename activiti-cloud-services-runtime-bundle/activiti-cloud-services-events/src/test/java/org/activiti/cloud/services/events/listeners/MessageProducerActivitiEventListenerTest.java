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

package org.activiti.cloud.services.events.listeners;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.converter.EventConverterContext;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MessageProducerActivitiEventListenerTest {

    @InjectMocks
    private MessageProducerActivitiEventListener listener;

    @Mock
    private EventConverterContext converterContext;

    @Mock
    private ProcessEngineEventsAggregator eventsAggregator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void onEventShouldConvertEventToProcessEngineEventAndAddItToAggregator() throws Exception {
        //given
        ActivitiEvent activitiEvent = mock(ActivitiEvent.class);

        ProcessEngineEvent processEngineEvent = mock(ProcessEngineEvent.class);
        given(converterContext.from(activitiEvent)).willReturn(processEngineEvent);

        //when
        listener.onEvent(activitiEvent);

        //then
        verify(eventsAggregator).add(processEngineEvent);
    }

    @Test
    public void onEventShouldIgnoreEventWhenItIsConvertedToANullProcessEngineEvent() throws Exception {
        //given
        ActivitiEvent activitiEvent = mock(ActivitiEvent.class);

        given(converterContext.from(activitiEvent)).willReturn(null);

        //when
        listener.onEvent(activitiEvent);

        //then
        verify(eventsAggregator, never()).add(null);
    }

}