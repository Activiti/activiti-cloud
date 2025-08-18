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
package org.activiti.cloud.services.notifications.graphql.ws.config;

import graphql.GraphQL;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass({ GraphQL.class, EnableWebSocketMessageBroker.class })
@ConditionalOnProperty(name = "spring.activiti.cloud.services.query.graphql.ws.enabled", matchIfMissing = true)
@PropertySources(
    {
        @PropertySource("classpath:META-INF/graphql-ws.properties"),
        @PropertySource(value = "classpath:graphql-ws.properties", ignoreResourceNotFound = true),
    }
)
public class GraphQLWebSocketMessageBrokerAutoConfiguration {}
