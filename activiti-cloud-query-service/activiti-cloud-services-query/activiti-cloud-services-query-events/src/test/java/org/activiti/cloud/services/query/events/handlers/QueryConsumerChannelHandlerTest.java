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
package org.activiti.cloud.services.query.events.handlers;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.api.task.model.impl.events.CloudTaskCreatedEventImpl;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class QueryConsumerChannelHandlerTest {

    private QueryConsumerChannelHandler consumer;

    @Mock
    private QueryEventHandlerContext eventHandlerContext;

    @Mock
    private QueryEventHandlerContextOptimizer optimizer;

    @Mock
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        consumer = new QueryConsumerChannelHandler(eventHandlerContext, optimizer, entityManager);
    }

    @Test
    void receiveShouldHandleReceivedEvent() {
        //given
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl();

        List<CloudRuntimeEvent<?, ?>> events = asList(processCreatedEvent, processStartedEvent);
        var messageId = UUID.randomUUID().toString();
        Map<String, Object> headers = Map.of("id", messageId);

        when(optimizer.optimize(events)).thenReturn(events);

        //when
        new TransactionTemplate(new PseudoTransactionManager()).executeWithoutResult(tx ->
            consumer.receive(events, headers)
        );

        //then
        verify(optimizer).optimize(events);
        verify(eventHandlerContext).handle(processCreatedEvent, processStartedEvent);
        verify(entityManager, atLeastOnce()).flush();
        verify(entityManager, atLeastOnce()).clear();
        assertThat(processCreatedEvent.getMessageId()).isEqualTo(messageId);
        assertThat(processCreatedEvent.getSequenceNumber()).isZero();
        assertThat(processStartedEvent.getMessageId()).isEqualTo(messageId);
        assertThat(processStartedEvent.getSequenceNumber()).isEqualTo(1);
    }

    @Test
    void receiveShouldHandleReceivedEvent_when_messageIdHeaderMissing() {
        //given
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        List<CloudRuntimeEvent<?, ?>> events = List.of(processCreatedEvent);
        Map<String, Object> headers = new HashMap<>();
        headers.put("id", null);

        when(optimizer.optimize(events)).thenReturn(events);

        //when
        new TransactionTemplate(new PseudoTransactionManager()).executeWithoutResult(tx ->
            consumer.receive(events, headers)
        );

        //then
        assertThat(processCreatedEvent.getMessageId()).isNull();
        assertThat(processCreatedEvent.getSequenceNumber()).isZero();
    }

    @Test
    void receiveShouldHandleEventsInChunks() {
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl();
        CloudTaskCreatedEventImpl taskCreatedEvent = new CloudTaskCreatedEventImpl();
        List<CloudRuntimeEvent<?, ?>> firstChunk = List.of(processCreatedEvent, processStartedEvent);
        List<CloudRuntimeEvent<?, ?>> secondChunk = List.of(taskCreatedEvent);
        List<CloudRuntimeEvent<?, ?>> events = List.of(taskCreatedEvent, processStartedEvent, processCreatedEvent);
        Map<String, Object> headers = Map.of("id", "message-id");

        consumer = new QueryConsumerChannelHandler(eventHandlerContext, optimizer, entityManager).chunkSize(2);

        when(optimizer.optimize(firstChunk)).thenReturn(firstChunk);
        when(optimizer.optimize(secondChunk)).thenReturn(secondChunk);

        new TransactionTemplate(new PseudoTransactionManager()).executeWithoutResult(tx ->
            consumer.receive(events, headers)
        );

        var inOrder = inOrder(optimizer, eventHandlerContext, entityManager);
        inOrder.verify(optimizer).optimize(firstChunk);
        inOrder.verify(eventHandlerContext).handle(processCreatedEvent, processStartedEvent);
        inOrder.verify(entityManager).flush();
        inOrder.verify(entityManager).clear();
        inOrder.verify(optimizer).optimize(secondChunk);
        inOrder.verify(eventHandlerContext).handle(taskCreatedEvent);
        inOrder.verify(entityManager).flush();
        inOrder.verify(entityManager).clear();
        assertThat(processCreatedEvent.getSequenceNumber()).isZero();
        assertThat(processStartedEvent.getSequenceNumber()).isEqualTo(1);
        assertThat(taskCreatedEvent.getSequenceNumber()).isEqualTo(2);
    }

    @Test
    void receiveShouldHandleEventsAsSingleChunkWithDefaultConstructor() {
        CloudProcessCreatedEventImpl processCreatedEvent = new CloudProcessCreatedEventImpl();
        CloudProcessStartedEventImpl processStartedEvent = new CloudProcessStartedEventImpl();
        List<CloudRuntimeEvent<?, ?>> events = List.of(processCreatedEvent, processStartedEvent);
        Map<String, Object> headers = Map.of("id", "message-id");

        consumer = new QueryConsumerChannelHandler(eventHandlerContext, optimizer, entityManager);

        when(optimizer.optimize(events)).thenReturn(events);

        new TransactionTemplate(new PseudoTransactionManager()).executeWithoutResult(tx ->
            consumer.receive(events, headers)
        );

        verify(optimizer).optimize(events);
        verify(eventHandlerContext).handle(processCreatedEvent, processStartedEvent);
        verify(entityManager).flush();
        verify(entityManager).clear();
    }
}
