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
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class QueryConsumerChannelHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryConsumerChannelHandler.class);

    private final QueryEventHandlerContext eventHandlerContext;
    private final QueryEventHandlerContextOptimizer optimizer;
    private final EntityManager entityManager;

    public QueryConsumerChannelHandler(
        QueryEventHandlerContext eventHandlerContext,
        QueryEventHandlerContextOptimizer optimizer,
        EntityManager entityManager
    ) {
        this.optimizer = optimizer;
        this.eventHandlerContext = eventHandlerContext;
        this.entityManager = entityManager;
    }

    public void receive(List<CloudRuntimeEvent<?, ?>> events) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "QUERY handler - begin tx with {} events, types={}",
                events == null ? 0 : events.size(),
                events == null ? List.of() : events.stream().map(e -> e.getEventType().name()).toList()
            );
        }
        registerAfterCompletionLogger();
        try {
            eventHandlerContext.handle(optimizer.optimize(events).toArray(new CloudRuntimeEvent[] {}));
            LOGGER.info("QUERY handler - end tx ok (will commit on return)");
        } catch (RuntimeException ex) {
            LOGGER.error("QUERY handler - tx will rollback due to: {}", ex.toString(), ex);
            throw ex;
        }
    }

    private void registerAfterCompletionLogger() {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    // 0 = COMMITTED, 1 = ROLLED_BACK, 2 = UNKNOWN
                    LOGGER.info("QUERY handler - tx afterCompletion status={}", status);
                    entityManager.clear();
                }
            }
        );
    }
}
