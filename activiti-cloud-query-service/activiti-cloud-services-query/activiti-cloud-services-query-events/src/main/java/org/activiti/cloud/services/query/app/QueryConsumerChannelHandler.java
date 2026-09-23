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

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Gatherers;
import org.activiti.cloud.api.events.CloudRuntimeEventSorter;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class QueryConsumerChannelHandler {

    private final QueryEventHandlerContext eventHandlerContext;
    private final QueryEventHandlerContextOptimizer optimizer;
    private final EntityManager entityManager;
    private final int chunkSize;

    public QueryConsumerChannelHandler(
        QueryEventHandlerContext eventHandlerContext,
        QueryEventHandlerContextOptimizer optimizer,
        EntityManager entityManager
    ) {
        this(eventHandlerContext, optimizer, entityManager, 100);
    }

    public QueryConsumerChannelHandler(
        QueryEventHandlerContext eventHandlerContext,
        QueryEventHandlerContextOptimizer optimizer,
        EntityManager entityManager,
        int chunkSize
    ) {
        this.optimizer = optimizer;
        this.eventHandlerContext = eventHandlerContext;
        this.entityManager = entityManager;
        this.chunkSize = chunkSize;
    }

    public void receive(List<CloudRuntimeEvent<?, ?>> events, Map<String, Object> headers) {
        final var counter = new AtomicInteger(0);

        CloudRuntimeEventSorter.sort(events)
            .stream()
            .<CloudRuntimeEvent<?, ?>>map(it -> enrichWithMessageMetadata(counter.getAndIncrement(), it, headers))
            .gather(Gatherers.windowFixed(chunkSize))
            .map(optimizer::optimize)
            .map(list -> list.toArray(CloudRuntimeEvent[]::new))
            .forEach(eventsChunk -> {
                try {
                    eventHandlerContext.handle(eventsChunk);
                    entityManager.flush();
                } finally {
                    entityManager.clear();
                }
            });
    }

    private CloudRuntimeEvent<?, ?> enrichWithMessageMetadata(
        Integer sequenceNumber,
        CloudRuntimeEvent<?, ?> event,
        Map<String, Object> headers
    ) {
        Object idHeader = headers.get("id");
        var messageId = idHeader != null ? idHeader.toString() : null;

        if (event instanceof CloudRuntimeEventImpl<?, ?> impl) {
            impl.setMessageId(messageId);
            impl.setSequenceNumber(sequenceNumber);
        }

        return event;
    }
}
