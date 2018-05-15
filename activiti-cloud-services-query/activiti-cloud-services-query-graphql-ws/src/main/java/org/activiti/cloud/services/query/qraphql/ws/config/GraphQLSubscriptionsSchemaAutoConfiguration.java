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
package org.activiti.cloud.services.query.qraphql.ws.config;

import com.introproventures.graphql.jpa.query.schema.GraphQLExecutor;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaExecutor;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.GraphQLStompRelayDataFetcherDestinationResolver;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayDataFetcher;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayDestinationResolver;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayPublisherFactory;
import org.activiti.cloud.services.query.qraphql.ws.schema.GraphQLSubscriptionSchemaBuilder;
import org.activiti.cloud.services.query.qraphql.ws.schema.GraphQLSubscriptionSchemaProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({GraphQLSubscriptionSchemaBuilder.class, StompRelayPublisherFactory.class})
@ConditionalOnProperty(name="spring.activiti.cloud.services.query.graphql.ws.enabled", matchIfMissing = true)
public class GraphQLSubscriptionsSchemaAutoConfiguration {

    @Configuration
    public static class DefaultGraphQLSubscriptionsSchemaConfiguration {

        @Autowired
        private GraphQLSubscriptionSchemaProperties subscriptionProperties;

        @Bean
        @ConditionalOnMissingBean
        public StompRelayDestinationResolver stompRelayDestinationResolver() {
            return new GraphQLStompRelayDataFetcherDestinationResolver(subscriptionProperties.getSubscriptionArgumentNames());
        }

        @Bean
        @ConditionalOnMissingBean
        public StompRelayDataFetcher stompRelayDataFetcher(StompRelayPublisherFactory stompRelayPublisherFactory) {
            return new StompRelayDataFetcher(stompRelayPublisherFactory);
        }

        @Bean
        @ConditionalOnMissingBean
        public GraphQLExecutor graphQLExecutor(final GraphQLSubscriptionSchemaBuilder subscriptionSchemaBuilder,
                                               StompRelayDataFetcher stompRelayDataFetcher) {
            subscriptionSchemaBuilder.withSubscription(subscriptionProperties.getSubscriptionFieldName(), stompRelayDataFetcher);

            return new GraphQLJpaExecutor(subscriptionSchemaBuilder.getGraphQLSchema());
        }
    }



}
