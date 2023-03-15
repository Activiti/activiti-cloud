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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProcessEngineEventsAggregatorTest {

    @InjectMocks
    @Spy
    private ProcessEngineEventsAggregator eventsAggregator;

    @Mock
    private MessageProducerCommandContextCloseListener closeListener;

    @Mock
    private CommandContext commandContext;

    @Captor
    private ArgumentCaptor<List<CloudRuntimeEvent<?, ?>>> eventsCaptor;

    @Mock
    private CloudRuntimeEvent<?, ?> event;

    @BeforeEach
    public void setUp() {
        when(eventsAggregator.getCurrentCommandContext()).thenReturn(commandContext);
    }

    @Test
    public void getCloseListenerClassShouldReturnMessageProducerCommandContextCloseListenerClass() {
        //when
        Class<MessageProducerCommandContextCloseListener> listenerClass = eventsAggregator.getCloseListenerClass();

        //then
        assertThat(listenerClass).isEqualTo(MessageProducerCommandContextCloseListener.class);
    }

    @Test
    public void getCloseListenerShouldReturnTheCloserListenerPassedInTheConstructor() {
        //when
        MessageProducerCommandContextCloseListener retrievedCloseListener = eventsAggregator.getCloseListener();

        //then
        assertThat(retrievedCloseListener).isEqualTo(closeListener);
    }

    @Test
    public void getAttributeKeyShouldReturnProcessEngineEvents() {
        //when
        String attributeKey = eventsAggregator.getAttributeKey();

        //then
        assertThat(attributeKey).isEqualTo(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS);
    }

    @Test
    public void addShouldAddTheEventEventToTheEventAttributeListWhenTheAttributeAlreadyExists() {
        //given
        ArrayList<CloudRuntimeEvent<?, ?>> currentEvents = new ArrayList<>();
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(currentEvents);

        //when
        eventsAggregator.add(event);

        //then
        assertThat(currentEvents).containsExactly(event);
        verify(commandContext, never())
            .addAttribute(eq(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS), any());
    }

    @Test
    public void addShouldCreateAnewListAndRegisterItAsAttributeWhenTheAttributeDoesNotExist() {
        //given
        given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS))
            .willReturn(null);

        //when
        eventsAggregator.add(event);

        //then
        verify(commandContext)
            .addAttribute(eq(MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS), eventsCaptor.capture());
        assertThat(eventsCaptor.getValue()).containsExactly(event);
    }

    @Test
    public void addShouldRegisterCloseListenerWhenItIsMissing() {
        //given
        given(commandContext.hasCloseListener(MessageProducerCommandContextCloseListener.class)).willReturn(false);

        //when
        eventsAggregator.add(event);

        //then
        verify(commandContext).addCloseListener(closeListener);
    }

    @Test
    public void addShouldNotRegisterCloseListenerWhenItIsAlreadyRegistered() {
        //given
        given(commandContext.hasCloseListener(MessageProducerCommandContextCloseListener.class)).willReturn(true);

        //when
        eventsAggregator.add(event);

        //then
        verify(commandContext, never()).addCloseListener(closeListener);
    }
}
