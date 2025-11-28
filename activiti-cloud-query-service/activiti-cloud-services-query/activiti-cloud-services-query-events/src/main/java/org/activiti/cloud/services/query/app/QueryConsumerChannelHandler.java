/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
import java.util.concurrent.locks.ReentrantLock;
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

    private static final Logger logger = LoggerFactory.getLogger(QueryConsumerChannelHandler.class);

    private final QueryEventHandlerContext eventHandlerContext;
    private final QueryEventHandlerContextOptimizer optimizer;
    private final EntityManager entityManager;
    private final ReentrantLock lock = new ReentrantLock();

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
        lock.lock();
        try {
            logger.info("[QUERY-TRACE] receive() method called");
            logger.info("[QUERY-TRACE] Received {} events", events != null ? events.size() : 0);

            if (events != null && !events.isEmpty()) {
                logger.info("[QUERY-TRACE] First event type: {}", events.get(0).getEventType());
                logger.info("[QUERY-TRACE] First event processInstanceId: {}", events.get(0).getProcessInstanceId());
            }

            afterCompletion(entityManager::clear);
            logger.info("[QUERY-TRACE] EntityManager clear callback registered");

            CloudRuntimeEvent<?, ?>[] eventsArray = optimizer.optimize(events).toArray(new CloudRuntimeEvent[] {});
            logger.info("[QUERY-TRACE] Events optimized, processing {} events", eventsArray.length);

            eventHandlerContext.handle(eventsArray);
            logger.info("[QUERY-TRACE] Processed {} events successfully", events != null ? events.size() : 0);
        } catch (Exception e) {
            logger.error("[QUERY-TRACE] ERROR processing events", e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private static void afterCompletion(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    action.run();
                }
            }
        );
    }
}
