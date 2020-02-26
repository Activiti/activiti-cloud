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

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Order(Ordered.HIGHEST_PRECEDENCE + 98)
public class JWSAuthenticationInterceptorConfigurer implements WebSocketMessageBrokerConfigurer {

    private static final String GRAPHQL_MESSAGE_TYPE = "graphQLMessageType";
    private static final String CONNECTION_INIT = "connection_init";
    private static final String X_AUTHORIZATION = "X-Authorization";
    private static final String BEARER = "Bearer";

    private List<String> headerValues = Arrays.asList(CONNECTION_INIT);
    private Predicate<SimpMessageHeaderAccessor> messageSelector = new DefaultMessageSelector();
    
    private final JWSAuthenticationManager authenticationManager;
    
    @Autowired
    public JWSAuthenticationInterceptorConfigurer(JWSAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
                                                                                       SimpMessageHeaderAccessor.class);
                if (accessor != null && messageSelector.test(accessor)) {
                    Optional.ofNullable(accessor.getHeader(X_AUTHORIZATION))
                            .map(String.class::cast)
                            .map(header -> header.replace(BEARER, "").trim())
                            .ifPresent(bearer -> {
                                Authentication jwsAuthToken = new JWSAuthentication(bearer);

                                Principal principal = authenticationManager.authenticate(jwsAuthToken);
                                
                                accessor.setUser(principal);
                            });
                }
                return message;
            }
        });
    }

    
    public void setHeaderValues(List<String> headerValues) {
        this.headerValues = headerValues;
    }

    
    public void setMessageSelector(Predicate<SimpMessageHeaderAccessor> messageSelector) {
        this.messageSelector = messageSelector;
    }
    
    class DefaultMessageSelector implements Predicate<SimpMessageHeaderAccessor> {
        
        @Override
        public boolean test(SimpMessageHeaderAccessor accessor) {
            Object value = accessor.getHeader(GRAPHQL_MESSAGE_TYPE);
            return headerValues.contains(value);
        }
    }
    
}