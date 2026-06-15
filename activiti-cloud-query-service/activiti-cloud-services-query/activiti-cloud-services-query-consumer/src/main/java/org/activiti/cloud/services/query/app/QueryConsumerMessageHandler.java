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
import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class QueryConsumerMessageHandler
    extends QueryConsumerChannelHandler
    implements Consumer<Message<List<CloudRuntimeEvent<?, ?>>>> {

    private final MessageChannel queryEventsChannel;

    public QueryConsumerMessageHandler(
        QueryEventHandlerContext eventHandlerContext,
        QueryEventHandlerContextOptimizer optimizer,
        EntityManager entityManager,
        MessageChannel queryEventsChannel
    ) {
        super(eventHandlerContext, optimizer, entityManager);
        this.queryEventsChannel = queryEventsChannel;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void accept(Message<List<CloudRuntimeEvent<?, ?>>> message) {
        beforeCommit(() -> queryEventsChannel.send(message));
        receive(message.getPayload(), message.getHeaders());
    }

    private static void beforeCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    action.run();
                }
            }
        );
    }
}
