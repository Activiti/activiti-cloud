/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.engine.impl.context.ExecutionContext;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class MayBeAddRootExecutionContext {

        private static final String ROOT_PROCESS_INSTANCE_ID = "root-pi-id";

        @Mock
        private ExecutionEntityManager executionEntityManager;

        @Mock
        private ExecutionEntity executionEntity;

        @Mock
        private ExecutionEntity rootProcessInstance;

        @Captor
        private ArgumentCaptor<ExecutionContext> executionContextCaptor;

        @BeforeEach
        public void setUpExecutionEntityManager() {
            when(commandContext.getExecutionEntityManager()).thenReturn(executionEntityManager);
        }

        @Test
        void shouldUseExecutionsOwnProcessInstanceWhenRootProcessInstanceIdIsNull() {
            // given — root process: getRootProcessInstanceId() is null, getProcessInstance() returns self
            when(executionEntity.getRootProcessInstanceId()).thenReturn(null);
            when(executionEntity.getProcessInstance()).thenReturn(executionEntity);
            given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT))
                .willReturn(null);

            // when
            eventsAggregator.mayBeAddRootExecutionContext(commandContext, executionEntity);

            // then — ROOT_EXECUTION_CONTEXT must be populated with the execution's own process instance,
            // otherwise the rootProcessInstanceId header is missing and PROCESS_CREATED ends up on a
            // different partition than its children.
            verify(commandContext)
                .addAttribute(
                    eq(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT),
                    executionContextCaptor.capture()
                );
            assertThat(executionContextCaptor.getValue()).isNotNull();
            assertThat(executionContextCaptor.getValue().getExecution()).isSameAs(executionEntity);
            verify(executionEntityManager, never()).findById(any());
        }

        @Test
        void shouldLookUpRootByIdWhenRootProcessInstanceIdIsNotNull() {
            // given — child execution: getRootProcessInstanceId() returns the root id
            when(executionEntity.getRootProcessInstanceId()).thenReturn(ROOT_PROCESS_INSTANCE_ID);
            when(executionEntityManager.findById(ROOT_PROCESS_INSTANCE_ID)).thenReturn(rootProcessInstance);
            given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT))
                .willReturn(null);

            // when
            eventsAggregator.mayBeAddRootExecutionContext(commandContext, executionEntity);

            // then — ROOT_EXECUTION_CONTEXT is created from the looked-up root process instance
            verify(commandContext)
                .addAttribute(
                    eq(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT),
                    executionContextCaptor.capture()
                );
            assertThat(executionContextCaptor.getValue()).isNotNull();
            assertThat(executionContextCaptor.getValue().getExecution()).isSameAs(rootProcessInstance);
        }

        @Test
        void shouldNotOverwriteWhenRootExecutionContextIsAlreadySet() {
            // given
            ExecutionContext existing = mock(ExecutionContext.class);
            given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT))
                .willReturn(existing);

            // when
            eventsAggregator.mayBeAddRootExecutionContext(commandContext, executionEntity);

            // then
            verify(commandContext, never())
                .addAttribute(eq(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT), any());
        }

        @Test
        void shouldNotSetRootExecutionContextWhenRootProcessInstanceCannotBeResolved() {
            // given — child execution but root id no longer findable (defensive guard)
            when(executionEntity.getRootProcessInstanceId()).thenReturn(ROOT_PROCESS_INSTANCE_ID);
            when(executionEntityManager.findById(ROOT_PROCESS_INSTANCE_ID)).thenReturn(null);
            given(commandContext.getGenericAttribute(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT))
                .willReturn(null);

            // when
            eventsAggregator.mayBeAddRootExecutionContext(commandContext, executionEntity);

            // then
            verify(commandContext, never())
                .addAttribute(eq(MessageProducerCommandContextCloseListener.ROOT_EXECUTION_CONTEXT), any());
        }
    }
}
