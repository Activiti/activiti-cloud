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
package org.activiti.cloud.services.notifications.qraphql.ws.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

public class WebSocketMessageBrokerSecurityConfigurer extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Value("${spring.activiti.cloud.services.notifications.graphql.ws.endpoint}")
    private String endpoint;

    @Value("${spring.activiti.cloud.services.notifications.graphql.ws.security.authorities}")
    private String[] authorities;

    @Override
    protected boolean sameOriginDisabled() {
        return true; // disable CSRF for web sockets
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages.simpMessageDestMatchers(endpoint).hasAnyRole(authorities);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String[] getAuthorities() {
        return authorities;
    }
}
