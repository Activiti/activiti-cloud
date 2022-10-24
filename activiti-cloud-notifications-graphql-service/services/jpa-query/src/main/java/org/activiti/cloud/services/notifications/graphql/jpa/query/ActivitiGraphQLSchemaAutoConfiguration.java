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
package org.activiti.cloud.services.notifications.graphql.jpa.query;

import com.introproventures.graphql.jpa.query.autoconfigure.GraphQLSchemaConfigurer;
import com.introproventures.graphql.jpa.query.autoconfigure.GraphQLShemaRegistration;
import com.introproventures.graphql.jpa.query.schema.JavaScalars;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaSchemaBuilder;
import graphql.GraphQL;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.VariableValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

/**
 * Spring Boot auto configuration of Activiti GraphQL Query Service components
 */
@Configuration
@ConditionalOnClass({GraphQL.class, ProcessInstanceEntity.class})
@ConditionalOnProperty(name = "spring.activiti.cloud.services.notifications.graphql.jpa-query.enabled", matchIfMissing = true)
public class ActivitiGraphQLSchemaAutoConfiguration {

    @Configuration
    @EntityScan(basePackageClasses = ProcessInstanceEntity.class)
    public static class ActivitiGraphQLSchemaConfigurer implements GraphQLSchemaConfigurer {

        private final EntityManager entityManager;

        public ActivitiGraphQLSchemaConfigurer(EntityManager entityManager) {
            this.entityManager = entityManager;

            JavaScalars.register(VariableValue.class,
                                 GraphQLScalarType.newScalar()
                                                  .name("VariableValue")
                                                  .description("VariableValue type")
                                                  .coercing(new JavaScalars.GraphQLObjectCoercing())
                                                  .build());
        }

        @Override
        public void configure(GraphQLShemaRegistration registry) {

            GraphQLSchema graphQLSchema = new GraphQLJpaSchemaBuilder(entityManager).name("Query")
                                                  .entityPath(ProcessInstanceEntity.class.getPackage().getName())
                                                  .description("Activiti Cloud Query Schema").build();

            registry.register(graphQLSchema);
        }
    }
}
