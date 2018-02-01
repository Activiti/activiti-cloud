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

@Configuration
@EnableActivitiGraphQLQueryService
@ConditionalOnGraphQLNotifications
public class GraphQLSubscriptionsSchemaConfiguration {

    private String graphQLSchemaFileName = "activiti.graphqls";

    private String graphQLSchemaSubscriptionFieldName = "ProcessEngineNotification";

    @Bean
    @ConditionalOnGraphQLNotifications
    public GraphQLSubscriptionSchemaBuilder graphqlSchemaBuilder(StompRelayPublisherFactory stompRelay) {

    	GraphQLSubscriptionSchemaBuilder schemaBuilder = new GraphQLSubscriptionSchemaBuilder(graphQLSchemaFileName);

    	schemaBuilder.withSubscription(graphQLSchemaSubscriptionFieldName, new StompRelayDataFetcher(stompRelay));

    	return schemaBuilder;
    }

    @Bean
    @ConditionalOnGraphQLNotifications
    public GraphQLExecutor graphQLExecutor(GraphQLSchemaBuilder querySchemaBuilder,
                                           GraphQLSubscriptionSchemaBuilder subscriptionSchemaBuilder)
    {
        // Merge query and subscriptions schemas into one
        GraphQLSchema querySchema = GraphQLSchema
            .newSchema(querySchemaBuilder.build())
            .subscription(subscriptionSchemaBuilder.getGraphQLSchema().getSubscriptionType())
            .build();

        return new GraphQLJpaExecutor(querySchema);
    }

}
