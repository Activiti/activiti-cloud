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

import java.util.function.Consumer;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContext;
import org.activiti.cloud.services.query.events.handlers.QueryEventHandlerContextOptimizer;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import reactor.core.publisher.Flux;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class QueryConsumerChannelHandler implements Consumer<Flux<List<CloudRuntimeEvent<?,?>>>> {

    private final QueryEventHandlerContext eventHandlerContext;
    private final QueryEventHandlerContextOptimizer optimizer;

    public QueryConsumerChannelHandler(QueryEventHandlerContext eventHandlerContext,
                                       QueryEventHandlerContextOptimizer optimizer) {
        this.optimizer = optimizer;
        this.eventHandlerContext = eventHandlerContext;
    }

    @Override
    public synchronized void accept(Flux<List<CloudRuntimeEvent<?, ?>>> listFlux) {
        listFlux.log().map(optimizer::optimize).subscribe(eventHandlerContext);
    }
}
