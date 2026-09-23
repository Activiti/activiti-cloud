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
package org.activiti.cloud.services.query.app;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
public class QueryConsumerMessageHandlerTest {

    private QueryConsumerMessageHandler consumer;

    @Mock
    private QueryEventHandlerContext eventHandlerContext;

    @Mock
    private QueryEventHandlerContextOptimizer optimizer;

    @Mock
    private EntityManager entityManager;

    @Mock
    private MessageChannel queryEventsChannel;

    @Mock
    private RecomputeEventCapturer recomputeEventCapturer;

    @BeforeEach
    void setUp() {
        consumer = new QueryConsumerMessageHandler(
            eventHandlerContext,
            optimizer,
            entityManager,
            queryEventsChannel,
            Optional.of(recomputeEventCapturer)
        );
    }

    @Test
    void handleMessageShouldHandleReceivedEventsAndPublishQueryEventMessage() {
        //given
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl();

        List<CloudRuntimeEvent<?, ?>> events = asList(processCreatedEvent, processStartedEvent);
        List<CloudRuntimeEvent<?, ?>> optimizedEvents = List.of(processStartedEvent);

        final var message = MessageBuilder.withPayload(events).setHeader("foo", "bar").build();

        when(optimizer.optimize(events)).thenReturn(optimizedEvents);

        //when
        new TransactionTemplate(new PseudoTransactionManager()).executeWithoutResult(tx -> consumer.accept(message));

        //then
        verify(optimizer).optimize(events);
        verify(eventHandlerContext).handle(processStartedEvent);
        verify(entityManager).clear();
        verify(queryEventsChannel).send(message);
        verify(recomputeEventCapturer).capture(events);
    }

    @Test
    void receiveShouldNotPublishQueryEventsMessageWhenEventHandlingFails() {
        //given
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        List<CloudRuntimeEvent<?, ?>> events = List.of(processCreatedEvent);

        final var message = MessageBuilder.withPayload(events).setHeader("foo", "bar").build();

        when(optimizer.optimize(events)).thenReturn(events);
        doThrow(new IllegalStateException("error")).when(eventHandlerContext).handle(processCreatedEvent);

        //when
        TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> consumer.accept(message)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("error");

        //then
        verify(eventHandlerContext).handle(processCreatedEvent);
        verify(entityManager).clear();
        verify(queryEventsChannel, never()).send(any(Message.class));
        verify(recomputeEventCapturer, never()).capture(any());
    }

    @Test
    void receiveShouldNotPublishQueryEventsMessageWhenTransactionIsRollback() {
        //given
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        List<CloudRuntimeEvent<?, ?>> events = List.of(processCreatedEvent);

        final var message = MessageBuilder.withPayload(events).setHeader("foo", "bar").build();

        when(optimizer.optimize(events)).thenReturn(events);

        //when
        TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
        assertThatThrownBy(() ->
            transactionTemplate.executeWithoutResult(tx -> {
                consumer.accept(message);

                throw new IllegalStateException("rollback");
            })
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("rollback");

        //then
        verify(eventHandlerContext).handle(processCreatedEvent);
        verify(entityManager).clear();
        verify(queryEventsChannel, never()).send(any(Message.class));
        verify(recomputeEventCapturer, never()).capture(any());
    }

    @Test
    void handleMessageShouldSucceed_whenRecomputeEventCapturerIsAbsent() {
        consumer = new QueryConsumerMessageHandler(
            eventHandlerContext,
            optimizer,
            entityManager,
            queryEventsChannel,
            Optional.empty()
        );
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl();
        List<CloudRuntimeEvent<?, ?>> events = List.of(processStartedEvent);
        final var message = MessageBuilder.withPayload(events).build();
        when(optimizer.optimize(events)).thenReturn(events);

        new TransactionTemplate(new PseudoTransactionManager()).executeWithoutResult(tx -> consumer.accept(message));

        verify(queryEventsChannel).send(message);
    }
}
