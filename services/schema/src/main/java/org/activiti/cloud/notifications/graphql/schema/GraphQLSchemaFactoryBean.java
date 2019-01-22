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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class GraphQLSchemaFactoryBean extends AbstractFactoryBean<GraphQLSchema>{
	
	private final GraphQLSchema[] managedGraphQLSchemas;

	public GraphQLSchemaFactoryBean(GraphQLSchema[] managedGraphQLSchemas) {
		this.managedGraphQLSchemas = managedGraphQLSchemas;
	}

	@Override
	protected GraphQLSchema createInstance() throws Exception {
		
		GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
		
		List<GraphQLFieldDefinition> mutations = Stream.of(managedGraphQLSchemas)
			.map(GraphQLSchema::getMutationType)
			.filter(Objects::nonNull)
			.map(GraphQLObjectType::getFieldDefinitions)
			.flatMap(children -> children.stream())
			.collect(Collectors.toList());

		List<GraphQLFieldDefinition> queries = Stream.of(managedGraphQLSchemas)
			.map(GraphQLSchema::getQueryType)
			.filter(Objects::nonNull)
			.map(GraphQLObjectType::getFieldDefinitions)
			.flatMap(children -> children.stream())
			.collect(Collectors.toList());
		
		List<GraphQLFieldDefinition> subscriptions = Stream.of(managedGraphQLSchemas)
			.map(GraphQLSchema::getSubscriptionType)
			.filter(Objects::nonNull)
			.map(GraphQLObjectType::getFieldDefinitions)
			.flatMap(children -> children.stream())
			.collect(Collectors.toList());

		if(!mutations.isEmpty())
			schemaBuilder.mutation(GraphQLObjectType.newObject().name("Mutation").fields(mutations));

		if(!queries.isEmpty())
			schemaBuilder.query(GraphQLObjectType.newObject().name("Query").fields(queries));

		if(!subscriptions.isEmpty())
			schemaBuilder.subscription(GraphQLObjectType.newObject().name("Subscription").fields(subscriptions));
		
		return schemaBuilder.build();
	}

	@Override
	public Class<?> getObjectType() {
		return GraphQLSchema.class;
	}

}
