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

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CommandContextEventsAggregatorTest {

    @InjectMocks
    @Spy
    private CommandContextEventsAggregator eventsAggregator;

    @Mock
    private MessageProducerCommandContextCloseListener closeListener;

    @Mock
    private CommandContext commandContext;

    @Captor
    private ArgumentCaptor<List<ProcessEngineEvent>> eventsCaptor;

    @Mock
    private ProcessEngineEvent event;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(eventsAggregator.getCurrentCommandContext()).thenReturn(commandContext);
    }

    @Test
    public void addShouldAddTheEventEventToTheEventAttributeListWhenTheAttributeAlreadyExists() throws Exception {
        //given
        ArrayList<ProcessEngineEvent> currentEvents = new ArrayList<>();
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS)).willReturn(currentEvents);

        //when
        eventsAggregator.add(event);

        //then
        assertThat(currentEvents).containsExactly(event);
        verify(commandContext, never()).addAttribute(eq(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS), any());
    }

    @Test
    public void addShouldCreateAnewListAndRegisterItAsAttributeWhenTheAttributeDoesNotExist() throws Exception {
        //given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS)).willReturn(null);

        //when
        eventsAggregator.add(event);

        //then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandContext).addAttribute(keyCaptor.capture(), eventsCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS);
        assertThat(eventsCaptor.getValue()).containsExactly(event);
    }

    @Test
    public void addShouldRegisterCloseListenerWhenItIsMissing() throws Exception {
        //given
        given(commandContext.hasCloseListener(MessageProducerCommandContextCloseListener.class)).willReturn(false);

        //when
        eventsAggregator.add(event);

        //then
        verify(commandContext).addCloseListener(closeListener);
    }

    @Test
    public void addShouldNotRegisterCloseListenerWhenItIsAlreadyRegistered() throws Exception {
        //given
        given(commandContext.hasCloseListener(MessageProducerCommandContextCloseListener.class)).willReturn(true);

        //when
        eventsAggregator.add(event);

        //then
        verify(commandContext, never()).addCloseListener(closeListener);
    }

}