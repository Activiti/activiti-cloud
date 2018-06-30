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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.activiti.cloud.services.query.qraphql.ws.config.GraphQLSubscriptionsSchemaAutoConfiguration.DefaultGraphQLSubscriptionsSchemaConfiguration;
import org.activiti.cloud.services.query.qraphql.ws.config.GraphQLWebSocketMessageBrokerAutoConfiguration.DefaultGraphQLWebSocketMessageBrokerConfiguration;
import org.springframework.context.annotation.Import;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Import({DefaultGraphQLSubscriptionsSchemaConfiguration.class,
    DefaultGraphQLWebSocketMessageBrokerConfiguration.class,
    GraphQLWebSocketMessageBrokerConfigurationProperties.AutoConfiguration.class})
public @interface EnableActivitiGraphQLNotifications {

}
