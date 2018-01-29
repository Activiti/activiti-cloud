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

package org.activiti.cloud.services.query.notifications.consumer;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.services.query.notifications.NotificationsGateway;
import org.activiti.cloud.services.query.notifications.NotificationsGatewaySupport;
import org.activiti.cloud.services.query.notifications.RoutingKeyResolver;
import org.activiti.cloud.services.query.notifications.config.NotificationsGatewayChannels;
import org.activiti.cloud.services.query.notifications.model.ProcessEngineNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;

public class NotificationsConsumerChannelHandler extends NotificationsGatewaySupport {

    private static Logger LOGGER = LoggerFactory.getLogger(NotificationsConsumerChannelHandler.class);

    private final ProcessEngineNotificationTransformer transformer;

    public NotificationsConsumerChannelHandler(NotificationsGateway notificationsGateway,
                                               ProcessEngineNotificationTransformer transformer,
                                               RoutingKeyResolver routingKeyResolver)
    {
        super(notificationsGateway, routingKeyResolver);

        this.transformer = transformer;

    }

    @StreamListener(NotificationsGatewayChannels.NOTIFICATIONS_CONSUMER)
    public synchronized void receive(List<Map<String,Object>> events) throws JsonProcessingException {

        List<ProcessEngineNotification> notifications = transformer.transform(events);

        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Transformed notifications {}", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(notifications));

        for (ProcessEngineNotification notification : notifications) {
            if(LOGGER.isDebugEnabled())
                LOGGER.debug("Handle {} from {}", notification, NotificationsGatewayChannels.NOTIFICATIONS_CONSUMER);

            notify(notification);
        }
    }




}
