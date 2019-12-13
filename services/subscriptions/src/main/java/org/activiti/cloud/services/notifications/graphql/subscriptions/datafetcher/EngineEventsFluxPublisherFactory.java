/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.springframework.messaging.Message;

import graphql.schema.DataFetchingEnvironment;
import reactor.core.publisher.Flux;
import reactor.util.Logger;
import reactor.util.Loggers;

public class EngineEventsFluxPublisherFactory implements EngineEventsPublisherFactory {

    private static Logger logger = Loggers.getLogger(EngineEventsFluxPublisherFactory.class);

    private final Flux<Message<List<EngineEvent>>> engineEventsFlux;
    private final EngineEventsPredicateFactory predicateFactory;
    
    public EngineEventsFluxPublisherFactory(Flux<Message<List<EngineEvent>>> engineEventsFlux,
                                            EngineEventsPredicateFactory predicateFactory) {
        this.engineEventsFlux = engineEventsFlux;
        this.predicateFactory = predicateFactory;
    }
    
    @Override
    public Flux<List<EngineEvent>> getPublisher(DataFetchingEnvironment environment) {
        Predicate<? super EngineEvent> predicate = predicateFactory.getPredicate(environment);

        return Flux.from(engineEventsFlux.log(logger, Level.CONFIG, true)
                                         .flatMapSequential(message -> Flux.fromIterable(message.getPayload())
                                                                           .filter(predicate)
                                                                           .collectList()
                                                                           .filter(list -> !list.isEmpty())));
    }
}
