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
package org.activiti.cloud.notifications.graphql.schema;

import java.util.ArrayList;
import java.util.List;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

@Configuration
@ConditionalOnClass(GraphQL.class)
public class GraphQLSchemaAutoConfiguration {

    private final List<GraphQLSchemaConfigurer> graphQLSchemaConfigurers = new ArrayList<>();
	
    @Autowired(required = true)
    public void setGraphQLSchemaConfigurers(List<GraphQLSchemaConfigurer> configurers) {
        if (!CollectionUtils.isEmpty(configurers)) {
        	graphQLSchemaConfigurers.addAll(configurers);
        }
    }
    
    @Bean
    @ConditionalOnMissingBean(GraphQLSchema.class)
    public GraphQLSchemaFactoryBean graphQLSchemaFactoryBean() {
    	GraphQLShemaRegistration graphQLShemaRegistration = new GraphQLShemaRegistration();

        for (GraphQLSchemaConfigurer configurer : graphQLSchemaConfigurers) {
            configurer.configure(graphQLShemaRegistration);
        }
        
        return new GraphQLSchemaFactoryBean(graphQLShemaRegistration.getManagedGraphQLSchemas());
        
    };
    
    
}
