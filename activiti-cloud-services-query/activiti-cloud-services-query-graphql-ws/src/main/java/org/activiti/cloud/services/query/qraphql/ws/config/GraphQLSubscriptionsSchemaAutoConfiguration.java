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
import com.introproventures.graphql.jpa.query.schema.GraphQLSchemaBuilder;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaExecutor;
import graphql.schema.GraphQLSchema;
import org.activiti.cloud.services.query.graphql.autoconfigure.EnableActivitiGraphQLQueryService;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayDataFetcher;
import org.activiti.cloud.services.query.qraphql.ws.datafetcher.StompRelayPublisherFactory;
import org.activiti.cloud.services.query.qraphql.ws.schema.GraphQLSubscriptionSchemaBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import graphql.GraphQL;

@Configuration
@ConditionalOnClass(GraphQL.class)
@EnableActivitiGraphQLQueryService
@ConditionalOnProperty(name="spring.activiti.cloud.services.notifications.gateway.enabled", matchIfMissing = true)
@ConditionalOnExpression("${spring.activiti.cloud.services.query.graphql.enabled}==null or ${spring.activiti.cloud.services.query.graphql.enabled}")
public class GraphQLSubscriptionsSchemaAutoConfiguration {


    @Configuration
    @ConditionalOnProperty(name="spring.activiti.cloud.services.notifications.gateway.enabled", matchIfMissing = true)
    @ConditionalOnExpression("${spring.activiti.cloud.services.query.graphql.enabled}==null or ${spring.activiti.cloud.services.query.graphql.enabled}")
    public static class DefaultGraphQLSubscriptionsSchemaConfiguration {

        private String graphQLSchemaFileName = "activiti.graphqls";

        private String graphQLSchemaSubscriptionFieldName = "ProcessEngineNotification";

        @Bean
        @ConditionalOnProperty(name="spring.activiti.cloud.services.notifications.gateway.enabled", matchIfMissing = true)
        @ConditionalOnExpression("${spring.activiti.cloud.services.query.graphql.enabled}==null or ${spring.activiti.cloud.services.query.graphql.enabled}")
        public GraphQLSubscriptionSchemaBuilder graphqlSchemaBuilder(StompRelayPublisherFactory stompRelay) {

            GraphQLSubscriptionSchemaBuilder schemaBuilder = new GraphQLSubscriptionSchemaBuilder(graphQLSchemaFileName);

            schemaBuilder.withSubscription(graphQLSchemaSubscriptionFieldName, new StompRelayDataFetcher(stompRelay));

            return schemaBuilder;
        }

        @Bean
        @ConditionalOnProperty(name="spring.activiti.cloud.services.notifications.gateway.enabled", matchIfMissing = true)
        @ConditionalOnExpression("${spring.activiti.cloud.services.query.graphql.enabled}==null or ${spring.activiti.cloud.services.query.graphql.enabled}")
        public GraphQLExecutor graphQLExecutor(final GraphQLSchemaBuilder querySchemaBuilder,
                                               final GraphQLSubscriptionSchemaBuilder subscriptionSchemaBuilder)
        {
            // Merge query and subscriptions schemas into one
            GraphQLSchema querySchema = GraphQLSchema
                    .newSchema(querySchemaBuilder.build())
                    .subscription(subscriptionSchemaBuilder.getGraphQLSchema().getSubscriptionType())
                    .build();

            return new GraphQLJpaExecutor(querySchema);
        }
    }



}
