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

import java.util.Collections;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MessageProducerCommandContextCloseListenerTest {

    @InjectMocks
    private MessageProducerCommandContextCloseListener closeListener;

    @Mock
    private ProcessEngineChannels producer;

    @Mock
    private MessageChannel auditChannel;

    @Mock
    private CommandContext commandContext;

    @Captor
    private ArgumentCaptor<Message<ProcessEngineEvent[]>> messageArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(producer.auditProducer()).thenReturn(auditChannel);
    }

    @Test
    public void closedShouldSendEventsRegisteredOnTheCommandContext() throws Exception {
        //given
        ProcessEngineEvent engineEvent = mock(ProcessEngineEvent.class);
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
                .willReturn(Collections.singletonList(engineEvent));

        //when
        closeListener.closed(commandContext);

        //then
        verify(auditChannel).send(messageArgumentCaptor.capture());
        assertThat(messageArgumentCaptor.getValue().getPayload()).containsExactly(engineEvent);
    }

    @Test
    public void closedShouldDoNothingWhenRegisteredEventsIsNull() throws Exception {
        //given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
                .willReturn(null);

        //when
        closeListener.closed(commandContext);

        //then
        verify(auditChannel, never()).send(any());
    }

    @Test
    public void closedShouldDoNothingWhenRegisteredEventsIsEmpty() throws Exception {
        //given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
                .willReturn(Collections.emptyList());

        //when
        closeListener.closed(commandContext);

        //then
        verify(auditChannel, never()).send(any());
    }

}