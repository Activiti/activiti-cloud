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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessCreatedEventImpl;
import org.activiti.cloud.api.process.model.impl.events.CloudProcessStartedEventImpl;
import org.activiti.cloud.services.query.app.QueryConsumerChannelHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class QueryConsumerChannelHandlerTest {

    @InjectMocks
    private QueryConsumerChannelHandler consumer;

    @Mock
    private QueryEventHandlerContext eventHandlerContext;

    @Mock
    private QueryEventHandlerContextOptimizer optimizer;

    @Mock
    private EntityManager entityManager;

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
        verify(entityManager).clear();
        assertThat(processCreatedEvent.getMessageId()).isEqualTo(messageId);
        assertThat(processCreatedEvent.getSequenceNumber()).isZero();
        assertThat(processStartedEvent.getMessageId()).isEqualTo(messageId);
        assertThat(processStartedEvent.getSequenceNumber()).isEqualTo(1);
    }
}
