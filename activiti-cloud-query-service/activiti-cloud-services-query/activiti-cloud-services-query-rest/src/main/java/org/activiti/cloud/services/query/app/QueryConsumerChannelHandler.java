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

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class QueryConsumerChannelHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(QueryConsumerChannelHandler.class);

    private final QueryEventHandlerContext eventHandlerContext;
    private final QueryEntityGraphFetchingOptimizer optimizer;

    public QueryConsumerChannelHandler(QueryEventHandlerContext eventHandlerContext,
                                       QueryEntityGraphFetchingOptimizer optimizer) {
        this.optimizer = optimizer;
        this.eventHandlerContext = eventHandlerContext;
    }

    @StreamListener(QueryConsumerChannels.QUERY_CONSUMER)
    public synchronized void receive(List<CloudRuntimeEvent<?, ?>> events) {
        optimizer.process(events);

        eventHandlerContext.handle(events.toArray(new CloudRuntimeEvent[]{}));
    }

}
