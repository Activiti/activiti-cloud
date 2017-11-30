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

import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.services.connectors.behavior.IntegrationProducerCommandContextCloseListener;
import org.activiti.services.connectors.model.IntegrationRequestEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class IntegrationEventsAggregatorTest {

    @Spy
    @InjectMocks
    private IntegrationEventsAggregator aggregator;

    @Mock
    private IntegrationProducerCommandContextCloseListener closeListener;

    @Mock
    private CommandContext commandContext;

    @Mock
    private Message<IntegrationRequestEvent> message;

    @Captor
    private ArgumentCaptor<List<Message<IntegrationRequestEvent>>> messagesCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(aggregator.getCurrentCommandContext()).thenReturn(commandContext);
    }

    @Test
    public void getCloseListenerClassShouldReturnIntegrationProducerCommandContextCloseListenerClass() throws Exception {
        //when
        Class<IntegrationProducerCommandContextCloseListener> listenerClass = aggregator.getCloseListenerClass();

        //then
        assertThat(listenerClass).isEqualTo(IntegrationProducerCommandContextCloseListener.class);
    }

    @Test
    public void getCloseListenerShouldReturnTheCloserListenerPassedInTheConstructor() throws Exception {
        //when
        IntegrationProducerCommandContextCloseListener retrievedCloseListener = aggregator.getCloseListener();

        //then
        assertThat(retrievedCloseListener).isEqualTo(closeListener);
    }

    @Test
    public void getAttributeKeyShouldReturnProcessEngineIntegrationEvents() throws Exception {
        //when
        String attributeKey = aggregator.getAttributeKey();

        //then
        assertThat(attributeKey).isEqualTo(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS);
    }

    @Test
    public void addShouldAddTheEventEventToTheEventAttributeListWhenTheAttributeAlreadyExists() throws Exception {
        //given
        ArrayList<Message<IntegrationRequestEvent>> currentMessages = new ArrayList<>();
        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS)).willReturn(currentMessages);

        //when
        aggregator.add(message);

        //then
        assertThat(currentMessages).containsExactly(message);
        verify(commandContext,
               never()).addAttribute(eq(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS),
                                     any());
    }

    @Test
    public void addShouldCreateAnewListAndRegisterItAsAttributeWhenTheAttributeDoesNotExist() throws Exception {
        //given
        given(commandContext.getGenericAttribute(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS)).willReturn(null);

        //when
        aggregator.add(message);

        //then
        verify(commandContext).addAttribute(eq(IntegrationProducerCommandContextCloseListener.PROCESS_ENGINE_INTEGRATION_EVENTS),
                                            messagesCaptor.capture());
        assertThat(messagesCaptor.getValue()).containsExactly(message);
    }

    @Test
    public void addShouldRegisterCloseListenerWhenItIsMissing() throws Exception {
        //given
        given(commandContext.hasCloseListener(IntegrationProducerCommandContextCloseListener.class)).willReturn(false);

        //when
        aggregator.add(message);

        //then
        verify(commandContext).addCloseListener(closeListener);
    }

    @Test
    public void addShouldNotRegisterCloseListenerWhenItIsAlreadyRegistered() throws Exception {
        //given
        given(commandContext.hasCloseListener(IntegrationProducerCommandContextCloseListener.class)).willReturn(true);

        //when
        aggregator.add(message);

        //then
        verify(commandContext,
               never()).addCloseListener(closeListener);
    }
}