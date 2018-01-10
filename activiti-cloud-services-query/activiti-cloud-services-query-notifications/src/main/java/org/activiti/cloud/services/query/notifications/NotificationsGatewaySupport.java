/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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
package org.activiti.cloud.services.query.notifications;

import org.activiti.cloud.services.query.notifications.model.ProcessEngineNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NotificationsGatewaySupport {

    private static Logger LOGGER = LoggerFactory.getLogger(NotificationsGatewaySupport.class);

    private final NotificationsGateway notificationsGateway;

    private final RoutingKeyResolver routingKeyResolver;

    public NotificationsGatewaySupport(NotificationsGateway notificationsGateway,
                                       RoutingKeyResolver routingKeyResolver) {
        this.notificationsGateway = notificationsGateway;
        this.routingKeyResolver = routingKeyResolver;
    }

    public void notify(ProcessEngineNotification event) {
        String routingKey =  routingKeyResolver.resolveRoutingKey(event);

        LOGGER.info("Routing to '{}' for event {} to {} channel", routingKey, event, notificationsGateway);

        notificationsGateway.send(event, routingKey);
    }

}
