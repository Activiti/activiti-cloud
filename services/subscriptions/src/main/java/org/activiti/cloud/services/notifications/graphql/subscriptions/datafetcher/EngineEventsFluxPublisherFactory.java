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
import java.util.Set;
import java.util.stream.Collectors;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.activiti.cloud.services.notifications.graphql.events.RoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Flux;

public class EngineEventsFluxPublisherFactory implements EngineEventsPublisherFactory {

    private static Logger log = LoggerFactory.getLogger(EngineEventsFluxPublisherFactory.class);

    private DataFetcherDestinationResolver destinationResolver = new AntPathDestinationResolver();

    private final Flux<Message<EngineEvent>> engineEventsFlux;
    private AntPathMatcher pathMatcher = new AntPathMatcher(".");
    private final RoutingKeyResolver routingKeyResolver;
    
    public EngineEventsFluxPublisherFactory(Flux<Message<EngineEvent>> engineEventsFlux,
                                            RoutingKeyResolver routingKeyResolver) {
        this.engineEventsFlux = engineEventsFlux;
        this.routingKeyResolver = routingKeyResolver;
    }
    
    @Override
    public Flux<EngineEvent> getPublisher(DataFetchingEnvironment environment) {
        Set<String> selections = resolveSelections(environment);
        List<String> destinations = destinationResolver.resolveDestinations(environment);
        
        log.info("Resolved selections: {} for destinations: {}", selections, destinations);

        return engineEventsFlux.map(Message::getPayload)
                               .filter(engineEvent -> {
                                   // filter events that do not match subscription arguments
                                   String path = routingKeyResolver.resolveRoutingKey(engineEvent);
                                   
                                   return destinations.stream()
                                                      .anyMatch(pattern -> pathMatcher.match(pattern, path));
                               })
                               .filter(engineEvent -> {
                                   // apply filter to events that do not match selections in the subscription
                                   return selections.stream().anyMatch(eventType -> engineEvent.containsKey(eventType));
                               });
    }
    
    /**
     * @param destinationResolver
     */
    public EngineEventsFluxPublisherFactory destinationResolver(DataFetcherDestinationResolver destinationResolver) {
        this.destinationResolver = destinationResolver;

        return this;
    }

    
    public EngineEventsFluxPublisherFactory pathMatcher(AntPathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
        
        return this;
    }
    
    protected Set<String> resolveSelections(DataFetchingEnvironment environment) {
        return environment.getField()
                          .getSelectionSet()
                          .getSelections()
                          .stream()
                          .filter(Field.class::isInstance)
                          .map(Field.class::cast)
                          .map(Field::getName)
                          .collect(Collectors.toSet());
    }
    
    
}
