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
package org.activiti.cloud.services.graphql.autoconfigure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.introproventures.graphql.jpa.query.schema.GraphQLExecutor;
import com.introproventures.graphql.jpa.query.schema.impl.GraphQLJpaExecutor;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Spring Boot auto configuration of Activiti GraphQL Query Service components
 */
@AutoConfiguration
@ConditionalOnClass({ GraphQL.class })
@ConditionalOnProperty(name = "spring.activiti.cloud.services.query.graphql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ActivitiGraphQLWebProperties.class)
@PropertySources(
    {
        @PropertySource(value = "classpath:META-INF/graphql.properties"),
        @PropertySource(value = "classpath:graphql.properties", ignoreResourceNotFound = true),
    }
)
public class ActivitiGraphQLAutoConfiguration {

    /**
     * Provides default configuration of Activiti GraphQL JPA Query Components
     */
    @Configuration
    public static class DefaultActivitiGraphQLJpaConfiguration {

        /**
         * This is needed because the graphql spec says that null values should be present
         */
        @Autowired
        public void configureObjectMapper(ObjectMapper objectMapper) {
            objectMapper
                .configOverride(Map.class)
                .setInclude(Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS));
        }

        @Bean
        @ConditionalOnMissingBean(GraphQLExecutor.class)
        public GraphQLExecutor graphQLExecutor(GraphQLSchema querySchema) {
            return new GraphQLJpaExecutor(querySchema);
        }
    }
}
