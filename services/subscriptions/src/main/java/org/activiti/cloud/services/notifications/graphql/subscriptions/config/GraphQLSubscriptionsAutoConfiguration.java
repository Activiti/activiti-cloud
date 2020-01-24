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
package org.activiti.cloud.services.notifications.graphql.subscriptions.config;

import java.util.List;

import org.activiti.cloud.services.notifications.graphql.events.RoutingKeyResolver;
import org.activiti.cloud.services.notifications.graphql.events.model.EngineEvent;
import org.activiti.cloud.services.notifications.graphql.subscriptions.GraphQLSubscriptionSchemaBuilder;
import org.activiti.cloud.services.notifications.graphql.subscriptions.GraphQLSubscriptionSchemaProperties;
import org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher.EngineEventsDestinationsPredicateFactory;
import org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher.EngineEventsFluxPublisherFactory;
import org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher.EngineEventsPredicateFactory;
import org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher.EngineEventsPublisherDataFetcher;
import org.activiti.cloud.services.notifications.graphql.subscriptions.datafetcher.EngineEventsPublisherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;

import com.introproventures.graphql.jpa.query.autoconfigure.GraphQLSchemaConfigurer;
import com.introproventures.graphql.jpa.query.autoconfigure.GraphQLShemaRegistration;
import graphql.GraphQL;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnClass({GraphQL.class, ReactorNettyTcpStompClient.class})
@ConditionalOnProperty(name="spring.activiti.cloud.services.notifications.graphql.subscriptions.enabled", matchIfMissing = true)
public class GraphQLSubscriptionsAutoConfiguration {

    @Configuration
    static class DefaultGraphQLSubscriptionsSchemaConfiguration {

        @Autowired
        private GraphQLSubscriptionSchemaProperties subscriptionProperties;

        @Bean
        @ConditionalOnMissingBean
        public EngineEventsPredicateFactory engineEventsPredicateFactory(RoutingKeyResolver routingKeyResolver) {
            return new EngineEventsDestinationsPredicateFactory(routingKeyResolver);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public EngineEventsPublisherFactory engineEventPublisherFactory(EngineEventsPredicateFactory engineEventsPredicateFactory,
                                                                        Flux<Message<List<EngineEvent>>> engineEventsFlux) {
            return new EngineEventsFluxPublisherFactory(engineEventsFlux, engineEventsPredicateFactory);
        }

        @Bean
        @ConditionalOnMissingBean
        public EngineEventsPublisherDataFetcher engineEventPublisherDataFetcher(EngineEventsPublisherFactory engineEventPublisherFactory) {
            return new EngineEventsPublisherDataFetcher(engineEventPublisherFactory);
        }
        
        @Bean
        @ConditionalOnMissingBean
        public GraphQLSubscriptionSchemaBuilder graphQLSubscriptionSchemaBuilder(EngineEventsPublisherDataFetcher engineEventPublisherDataFetcher) {
            GraphQLSubscriptionSchemaBuilder schemaBuilder = new GraphQLSubscriptionSchemaBuilder(subscriptionProperties.getGraphqls());

            schemaBuilder.withSubscription(subscriptionProperties.getSubscriptionFieldName(),
                                           engineEventPublisherDataFetcher);

            return schemaBuilder;
        }
    }

    @Configuration
    static class DefaultGraphQLSubscriptionsConfigurer implements GraphQLSchemaConfigurer {

        @Autowired
        private GraphQLSubscriptionSchemaBuilder graphQLSubscriptionSchemaBuilder;

        @Override
        public void configure(GraphQLShemaRegistration registry) {
            registry.register(graphQLSubscriptionSchemaBuilder.getGraphQLSchema());
            
        }
    }


}
