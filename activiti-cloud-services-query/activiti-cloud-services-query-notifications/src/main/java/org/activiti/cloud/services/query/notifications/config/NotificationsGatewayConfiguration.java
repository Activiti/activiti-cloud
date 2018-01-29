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
package org.activiti.cloud.services.query.notifications.config;

import java.util.Arrays;

import org.activiti.cloud.services.query.notifications.NotificationsGateway;
import org.activiti.cloud.services.query.notifications.RoutingKeyResolver;
import org.activiti.cloud.services.query.notifications.consumer.NotificationsConsumerChannelHandler;
import org.activiti.cloud.services.query.notifications.consumer.ProcessEngineNotificationTransformer;
import org.activiti.cloud.services.query.notifications.graphql.GraphQLProcessEngineNotificationTransformer;
import org.activiti.cloud.services.query.notifications.graphql.SpELTemplateRoutingKeyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

/**
 * Notification Gateway configuration that enables messaging channel bindings
 * and scans for MessagingGateway on interfaces to create GatewayProxyFactoryBeans.
 *
 */
@Configuration
@EnableBinding(NotificationsGatewayChannels.class)
@IntegrationComponentScan(basePackageClasses=NotificationsGateway.class)
@EnableConfigurationProperties(ActivitiNotificationsGatewayProperties.class)
public class NotificationsGatewayConfiguration {

    @Autowired
    private ActivitiNotificationsGatewayProperties properties;

    @Bean
    public RoutingKeyResolver routingKeyResolver() {
        return new SpELTemplateRoutingKeyResolver();
    }

    @Bean
    public ProcessEngineNotificationTransformer processEngineEventNotificationTransformer() {
        return new GraphQLProcessEngineNotificationTransformer(
            Arrays.asList(properties.getProcessEngineEventAttributeKeys().split(",")),
            properties.getProcessEngineEventTypeKey()
        );
    }

    @Bean
    public NotificationsConsumerChannelHandler notificationsConsumerChannelHandler(
               NotificationsGateway notificationsGateway,
               ProcessEngineNotificationTransformer processEngineNotificationTransformer,
               RoutingKeyResolver routingKeyResolver)
    {
        return new NotificationsConsumerChannelHandler(
                       notificationsGateway,
                       processEngineNotificationTransformer,
                       routingKeyResolver);
    }

}
