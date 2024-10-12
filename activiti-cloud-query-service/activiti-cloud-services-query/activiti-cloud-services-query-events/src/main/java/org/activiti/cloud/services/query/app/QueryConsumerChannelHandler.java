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
package org.activiti.cloud.services.query.app;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class QueryConsumerChannelHandler {

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

    public synchronized void receive(List<CloudRuntimeEvent<?, ?>> events) {
        afterCompletion(entityManager::clear);
        eventHandlerContext.handle(optimizer.optimize(events).toArray(new CloudRuntimeEvent[] {}));
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
